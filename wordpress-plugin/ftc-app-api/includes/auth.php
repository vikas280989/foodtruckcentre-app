<?php
/**
 * Customer accounts for the app.
 * - Customers are WordPress users with the "FTC Customer" role (no wp-admin access).
 * - Passwords are hashed and checked by WordPress itself.
 * - The app receives a random session token; only its SHA-256 hash is stored (ftc_app_tokens table).
 */
if (!defined('ABSPATH')) {
    exit;
}

const FTC_APP_ROLE = 'ftc_customer';
const FTC_APP_TOKEN_DAYS = 180;

function ftc_app_add_role(): void
{
    if (!get_role(FTC_APP_ROLE)) {
        add_role(FTC_APP_ROLE, 'FTC Customer', ['read' => true]);
    }
}

function ftc_app_is_customer(?WP_User $u): bool
{
    return $u instanceof WP_User && $u->exists() && in_array(FTC_APP_ROLE, (array) $u->roles, true);
}

// Customers never see wp-admin or the admin bar if they log in on the website.
add_action('admin_init', function () {
    $u = wp_get_current_user();
    if (ftc_app_is_customer($u) && count((array) $u->roles) === 1 && !wp_doing_ajax()) {
        wp_safe_redirect(home_url('/'));
        exit;
    }
});
add_filter('show_admin_bar', function ($show) {
    $u = wp_get_current_user();
    return (ftc_app_is_customer($u) && count((array) $u->roles) === 1) ? false : $show;
});

/* ---------- Tokens ---------- */

function ftc_app_issue_token(int $userId): string
{
    global $wpdb;
    $token = bin2hex(random_bytes(32));
    $wpdb->insert(ftc_app_tokens_table(), [
        'token_hash' => hash('sha256', $token),
        'user_id' => $userId,
        'created_at' => current_time('mysql', true),
        'expires_at' => gmdate('Y-m-d H:i:s', time() + FTC_APP_TOKEN_DAYS * DAY_IN_SECONDS),
    ]);
    // Keep at most 10 sessions per customer (oldest removed).
    $ids = $wpdb->get_col($wpdb->prepare('SELECT token_hash FROM ' . ftc_app_tokens_table() . ' WHERE user_id = %d ORDER BY created_at DESC LIMIT 100 OFFSET 10', $userId));
    foreach ($ids as $h) {
        $wpdb->delete(ftc_app_tokens_table(), ['token_hash' => $h]);
    }
    return $token;
}

function ftc_app_request_token(): string
{
    $t = (string) ($_SERVER['HTTP_X_FTC_TOKEN'] ?? '');
    if ($t === '') {
        $auth = (string) ($_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
        if (stripos($auth, 'Bearer ') === 0) {
            $t = trim(substr($auth, 7));
        }
    }
    return preg_match('/^[a-f0-9]{64}$/', $t) ? $t : '';
}

/** The logged-in customer for this request, or null. */
function ftc_app_current_customer(): ?WP_User
{
    global $wpdb;
    $token = ftc_app_request_token();
    if ($token === '') {
        return null;
    }
    $hash = hash('sha256', $token);
    $row = $wpdb->get_row($wpdb->prepare('SELECT * FROM ' . ftc_app_tokens_table() . ' WHERE token_hash = %s', $hash), ARRAY_A);
    if (!$row || strtotime($row['expires_at'] . ' UTC') < time()) {
        return null;
    }
    $u = get_user_by('id', (int) $row['user_id']);
    if (!ftc_app_is_customer($u ?: null)) {
        return null;
    }
    if (empty($row['last_used']) || strtotime($row['last_used'] . ' UTC') < time() - 3600) {
        $wpdb->update(ftc_app_tokens_table(), ['last_used' => current_time('mysql', true)], ['token_hash' => $hash]);
    }
    return $u;
}

function ftc_app_require_customer(): WP_User
{
    $u = ftc_app_current_customer();
    if (!$u) {
        ftc_app_json(401, ['ok' => false, 'error' => 'login_required', 'message' => 'Please log in to continue. If you have an older version of the app, please update it.']);
    }
    return $u;
}

function ftc_app_profile(WP_User $u): array
{
    return [
        'name' => (string) $u->display_name,
        'email' => (string) $u->user_email,
        'phone' => (string) get_user_meta($u->ID, 'ftc_phone', true),
    ];
}

/* ---------- Request helpers ---------- */

function ftc_app_read_json(): array
{
    $type = strtolower((string) ($_SERVER['CONTENT_TYPE'] ?? ''));
    if (!str_starts_with($type, 'application/json')) {
        ftc_app_json(415, ['ok' => false, 'error' => 'unsupported_media_type', 'message' => 'Unsupported request.']);
    }
    $max = 16384;
    $raw = file_get_contents('php://input', false, null, 0, $max + 1);
    if ($raw === false || strlen($raw) > $max) {
        ftc_app_json(413, ['ok' => false, 'error' => 'too_large', 'message' => 'Your request is too large.']);
    }
    $data = json_decode($raw, true, 8);
    if (!is_array($data) || array_is_list($data)) {
        ftc_app_json(400, ['ok' => false, 'error' => 'bad_request', 'message' => 'Invalid request.']);
    }
    return $data;
}

function ftc_app_require_method(string $method): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== $method) {
        header('Allow: ' . $method);
        ftc_app_json(405, ['ok' => false, 'error' => 'method_not_allowed', 'message' => 'Method not allowed.']);
    }
}

function ftc_app_password_error(string $pw): ?string
{
    if (strlen($pw) < 8) {
        return 'Use at least 8 characters.';
    }
    if (strlen($pw) > 200) {
        return 'That password is too long.';
    }
    if (!preg_match('/[A-Za-z]/', $pw) || !preg_match('/\d/', $pw)) {
        return 'Use at least one letter and one number.';
    }
    return null;
}

/* ---------- Endpoints ---------- */

function ftc_app_auth_register(): void
{
    ftc_app_require_method('POST');
    $data = ftc_app_read_json();
    if (!ftc_app_rate_limit_ok('register', 10, 30)) {
        header('Retry-After: 600');
        ftc_app_json(429, ['ok' => false, 'error' => 'rate_limited', 'message' => 'Too many attempts. Please wait a few minutes and try again.']);
    }
    if (ftc_app_clean($data['website'] ?? '', 200) !== '') {
        ftc_app_json(400, ['ok' => false, 'error' => 'rejected', 'message' => 'Please check your details and try again.']);
    }
    $errors = [];
    $name = ftc_app_clean($data['name'] ?? '', 100);
    if (mb_strlen($name) < 2) {
        $errors['name'] = 'Please enter your full name.';
    }
    $email = strtolower(ftc_app_clean($data['email'] ?? '', 254));
    if (!ftc_app_valid_email($email) || !is_email($email)) {
        $errors['email'] = 'Please enter a valid email address.';
    }
    $phone = ftc_app_clean($data['phone'] ?? '', 20);
    if (!ftc_app_valid_phone($phone)) {
        $errors['phone'] = 'Please enter a valid phone number.';
    }
    $password = is_string($data['password'] ?? null) ? $data['password'] : '';
    if ($pe = ftc_app_password_error($password)) {
        $errors['password'] = $pe;
    }
    if (!$errors && email_exists($email)) {
        $errors['email'] = 'An account with this email already exists. Please log in, or use "Forgot password".';
    }
    if ($errors) {
        ftc_app_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Please check the highlighted fields.', 'fields' => $errors]);
    }

    $login = sanitize_user(strstr($email, '@', true), true) ?: 'customer';
    $base = substr($login, 0, 40);
    while (username_exists($login)) {
        $login = $base . '-' . strtolower(wp_generate_password(5, false));
    }
    $userId = wp_insert_user([
        'user_login' => $login,
        'user_email' => $email,
        'user_pass' => $password,
        'display_name' => $name,
        'first_name' => strtok($name, ' ') ?: $name,
        'role' => FTC_APP_ROLE,
        'show_admin_bar_front' => 'false',
    ]);
    if (is_wp_error($userId)) {
        error_log('[ftc-app-api] register failed: ' . $userId->get_error_code());
        ftc_app_json(503, ['ok' => false, 'error' => 'register_failed', 'message' => 'We couldn\'t create your account right now. Please try again later.']);
    }
    update_user_meta($userId, 'ftc_phone', $phone);
    update_user_meta($userId, 'ftc_registered_via', strtolower(ftc_app_platform()) . '-app');
    $u = get_user_by('id', $userId);
    ftc_app_json(200, ['ok' => true, 'token' => ftc_app_issue_token((int) $userId), 'profile' => ftc_app_profile($u)]);
}

function ftc_app_auth_login(): void
{
    ftc_app_require_method('POST');
    $data = ftc_app_read_json();
    if (!ftc_app_rate_limit_ok('login', 10, 60)) {
        header('Retry-After: 600');
        ftc_app_json(429, ['ok' => false, 'error' => 'rate_limited', 'message' => 'Too many attempts. Please wait a few minutes and try again.']);
    }
    $email = strtolower(ftc_app_clean($data['email'] ?? '', 254));
    $password = is_string($data['password'] ?? null) ? $data['password'] : '';
    $generic = ['ok' => false, 'error' => 'invalid_login', 'message' => 'Email or password is incorrect.'];
    if ($email === '' || $password === '') {
        ftc_app_json(401, $generic);
    }
    $u = get_user_by('email', $email);
    if (!$u || !ftc_app_is_customer($u)) {
        wp_check_password($password, '$P$Bxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'); // similar timing for unknown emails
        ftc_app_json(401, $generic);
    }
    // Per-account lockout after 5 wrong passwords (15 minutes).
    $fails = (array) get_user_meta($u->ID, 'ftc_login_fails', true);
    $recent = array_values(array_filter($fails, fn($t) => is_int($t) && $t > time() - 900));
    if (count($recent) >= 5) {
        ftc_app_json(429, ['ok' => false, 'error' => 'locked', 'message' => 'Too many wrong passwords. Please wait 15 minutes or use "Forgot password".']);
    }
    if (!wp_check_password($password, $u->user_pass, $u->ID)) {
        $recent[] = time();
        update_user_meta($u->ID, 'ftc_login_fails', $recent);
        ftc_app_json(401, $generic);
    }
    delete_user_meta($u->ID, 'ftc_login_fails');
    ftc_app_json(200, ['ok' => true, 'token' => ftc_app_issue_token($u->ID), 'profile' => ftc_app_profile($u)]);
}

function ftc_app_auth_logout(): void
{
    global $wpdb;
    ftc_app_require_method('POST');
    $token = ftc_app_request_token();
    if ($token !== '') {
        $wpdb->delete(ftc_app_tokens_table(), ['token_hash' => hash('sha256', $token)]);
    }
    ftc_app_json(200, ['ok' => true]);
}

function ftc_app_auth_forgot(): void
{
    ftc_app_require_method('POST');
    $data = ftc_app_read_json();
    if (!ftc_app_rate_limit_ok('forgot', 3, 10)) {
        header('Retry-After: 600');
        ftc_app_json(429, ['ok' => false, 'error' => 'rate_limited', 'message' => 'Too many attempts. Please wait a few minutes and try again.']);
    }
    $email = strtolower(ftc_app_clean($data['email'] ?? '', 254));
    if (!ftc_app_valid_email($email)) {
        ftc_app_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Please enter a valid email address.', 'fields' => ['email' => 'Please enter a valid email address.']]);
    }
    $u = get_user_by('email', $email);
    if ($u && ftc_app_is_customer($u)) {
        // WordPress emails its standard "reset your password" link.
        $r = retrieve_password($u->user_login);
        if (is_wp_error($r)) {
            error_log('[ftc-app-api] password reset email failed: ' . $r->get_error_code());
        }
    }
    // Same answer whether or not the account exists (no account discovery).
    ftc_app_json(200, ['ok' => true, 'message' => 'If an account exists for that email, we\'ve sent a link to reset your password. Check your inbox and spam folder.']);
}

function ftc_app_me(): void
{
    $u = ftc_app_require_customer();
    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'GET') {
        ftc_app_json(200, ['ok' => true, 'profile' => ftc_app_profile($u)]);
    }
    ftc_app_require_method('POST');
    $data = ftc_app_read_json();
    $errors = [];
    $name = ftc_app_clean($data['name'] ?? '', 100);
    if (mb_strlen($name) < 2) {
        $errors['name'] = 'Please enter your full name.';
    }
    $phone = ftc_app_clean($data['phone'] ?? '', 20);
    if (!ftc_app_valid_phone($phone)) {
        $errors['phone'] = 'Please enter a valid phone number.';
    }
    if ($errors) {
        ftc_app_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Please check the highlighted fields.', 'fields' => $errors]);
    }
    wp_update_user(['ID' => $u->ID, 'display_name' => $name, 'first_name' => strtok($name, ' ') ?: $name]);
    update_user_meta($u->ID, 'ftc_phone', $phone);
    ftc_app_json(200, ['ok' => true, 'profile' => ftc_app_profile(get_user_by('id', $u->ID))]);
}

function ftc_app_me_delete(): void
{
    global $wpdb;
    ftc_app_require_method('POST');
    $u = ftc_app_require_customer();
    $data = ftc_app_read_json();
    $password = is_string($data['password'] ?? null) ? $data['password'] : '';
    if (!wp_check_password($password, $u->user_pass, $u->ID)) {
        ftc_app_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Password is incorrect.', 'fields' => ['password' => 'Password is incorrect.']]);
    }
    require_once ABSPATH . 'wp-admin/includes/user.php';
    // Enquiries already sent stay as business records but are no longer linked to an account.
    $wpdb->update(ftc_app_table(), ['user_id' => 0], ['user_id' => $u->ID]);
    $wpdb->delete(ftc_app_tokens_table(), ['user_id' => $u->ID]);
    wp_delete_user($u->ID);
    ftc_app_json(200, ['ok' => true]);
}

function ftc_app_my_enquiries(): void
{
    global $wpdb;
    ftc_app_require_method('GET');
    $u = ftc_app_require_customer();
    $rows = $wpdb->get_results($wpdb->prepare(
        'SELECT enquiry_ref, type, stage, status, event_date, details, created_at FROM ' . ftc_app_table() . ' WHERE user_id = %d ORDER BY id DESC LIMIT 100',
        $u->ID
    ), ARRAY_A);
    $stages = ftc_app_stages();
    $items = [];
    foreach ($rows ?: [] as $r) {
        $details = json_decode((string) $r['details'], true) ?: [];
        if ($r['type'] === 'quote') {
            $title = 'Catering quote';
            $ev = $details['Event details'] ?? [];
            $summary = trim(implode(' · ', array_filter([
                $ev['Event type'] ?? '',
                isset($ev['Number of meals']) ? $ev['Number of meals'] . ' meals' : '',
                $details['Cuisines (max. 3)']['Selected cuisines'] ?? '',
            ])));
        } else {
            $title = (string) ($details['Message']['Topic'] ?? 'Message');
            $summary = mb_substr((string) ($details['Message']['Message'] ?? ''), 0, 120);
        }
        $items[] = [
            'enquiry_id' => $r['enquiry_ref'],
            'type' => $r['type'],
            'title' => $title,
            'summary' => $summary,
            'event_date' => $r['event_date'] ?: null,
            'submitted_at' => gmdate('c', strtotime($r['created_at'] . ' UTC')),
            'stage' => $r['stage'],
            'stage_label' => $stages[$r['stage']] ?? 'New',
            'delivered' => $r['status'] === 'sent',
        ];
    }
    ftc_app_json(200, ['ok' => true, 'enquiries' => $items]);
}
