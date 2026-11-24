<?php
/**
 * The app's endpoints. They answer at the addresses already built into the released APK:
 *   POST /app-api/v1/quote.php     POST /app-api/v1/contact.php     GET /app-api/v1/app-config.php
 * and also at /wp-json/ftc/v1/quote | contact | app-config for future app versions.
 * Request and response formats are identical to the standalone app-api.
 */
if (!defined('ABSPATH')) {
    exit;
}

function ftc_app_maybe_handle_request(): void
{
    $path = (string) parse_url($_SERVER['REQUEST_URI'] ?? '', PHP_URL_PATH);
    if (!preg_match('#/app-api/v1/(quote|contact|app-config)\.php$#', $path, $m)) {
        return;
    }
    ftc_app_dispatch($m[1]);
}

function ftc_app_register_rest_routes(): void
{
    foreach (['quote' => 'POST', 'contact' => 'POST', 'app-config' => 'GET'] as $route => $method) {
        register_rest_route('ftc/v1', '/' . $route, [
            'methods' => $method,
            'permission_callback' => '__return_true', // public: the app has no login; protected by the checks below
            'callback' => function () use ($route) {
                ftc_app_dispatch($route);
            },
        ]);
    }
}

function ftc_app_dispatch(string $endpoint): void
{
    if (!ftc_app_is_https()) {
        ftc_app_json(403, ['ok' => false, 'error' => 'https_required', 'message' => 'A secure connection is required.']);
    }
    if ($endpoint === 'app-config') {
        if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') {
            header('Allow: GET');
            ftc_app_json(405, ['ok' => false, 'error' => 'method_not_allowed', 'message' => 'Method not allowed.']);
        }
        $contact = (string) get_option('ftc_app_contact_email', '');
        ftc_app_json(200, [
            'ok' => true,
            'api_version' => FTC_APP_VERSION,
            'contact_email' => ftc_app_valid_email($contact) ? $contact : null,
        ]);
    }
    ftc_app_handle_submission($endpoint);
}

function ftc_app_handle_submission(string $endpoint): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        header('Allow: POST');
        ftc_app_json(405, ['ok' => false, 'error' => 'method_not_allowed', 'message' => 'Method not allowed.']);
    }
    $type = strtolower((string) ($_SERVER['CONTENT_TYPE'] ?? ''));
    if (!str_starts_with($type, 'application/json')) {
        ftc_app_json(415, ['ok' => false, 'error' => 'unsupported_media_type', 'message' => 'Unsupported request.']);
    }
    $max = 16384;
    if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > $max) {
        ftc_app_json(413, ['ok' => false, 'error' => 'too_large', 'message' => 'Your message is too long.']);
    }
    $raw = file_get_contents('php://input', false, null, 0, $max + 1);
    if ($raw === false || strlen($raw) > $max) {
        ftc_app_json(413, ['ok' => false, 'error' => 'too_large', 'message' => 'Your message is too long.']);
    }
    $data = json_decode($raw, true, 8);
    if (!is_array($data) || array_is_list($data)) {
        ftc_app_json(400, ['ok' => false, 'error' => 'bad_request', 'message' => 'Invalid request.']);
    }

    if (!ftc_app_rate_limit_ok()) {
        header('Retry-After: 600');
        ftc_app_json(429, ['ok' => false, 'error' => 'rate_limited', 'message' => 'Too many requests. Please wait a few minutes and try again.']);
    }

    // Bot checks: hidden honeypot must be empty; form must not be completed implausibly fast.
    $elapsed = $data['elapsed_seconds'] ?? null;
    if (ftc_app_clean($data['website'] ?? '', 200) !== '' || !is_numeric($elapsed) || (int) $elapsed < (int) get_option('ftc_app_min_fill', 4)) {
        ftc_app_json(400, ['ok' => false, 'error' => 'rejected', 'message' => 'Please take a moment to check your details and submit again.']);
    }

    $sid = strtolower((string) ($data['submission_id'] ?? ''));
    if (!preg_match('/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/', $sid)) {
        ftc_app_json(400, ['ok' => false, 'error' => 'bad_request', 'message' => 'Invalid request. Please restart the app and try again.']);
    }

    $e = $endpoint === 'quote' ? ftc_app_validate_quote($data) : ftc_app_validate_contact($data);
    if (isset($e['errors'])) {
        ftc_app_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Please check the highlighted fields.', 'fields' => $e['errors']]);
    }

    // Duplicate protection: same submission ID already delivered, or identical content delivered in the last 30 min.
    $submissionHash = hash('sha256', $endpoint . '|' . $sid);
    $existing = ftc_app_find_by_submission($submissionHash);
    if ($existing && $existing['status'] === 'sent') {
        ftc_app_json(200, ['ok' => true, 'enquiry_id' => $existing['enquiry_ref'], 'duplicate' => true]);
    }
    $dup = ftc_app_find_recent_duplicate($e['fingerprint']);
    if ($dup) {
        ftc_app_json(200, ['ok' => true, 'enquiry_id' => $dup['enquiry_ref'], 'duplicate' => true]);
    }

    // Store first, then send. Success is reported only when the mail system accepted the message.
    $row = ftc_app_save_enquiry($submissionHash, $e, $existing);
    if (($row['status'] ?? '') === 'sent') {
        ftc_app_json(200, ['ok' => true, 'enquiry_id' => $row['enquiry_ref'], 'duplicate' => true]);
    }
    [$sent, $error] = ftc_app_send($row);
    ftc_app_mark($row, $sent, $error);
    if ($sent) {
        ftc_app_json(200, ['ok' => true, 'enquiry_id' => $row['enquiry_ref']]);
    }
    error_log('[ftc-app-api] delivery failed for ' . $row['enquiry_ref'] . ' (will retry automatically)');
    ftc_app_json(502, [
        'ok' => false,
        'error' => 'delivery_failed',
        'message' => 'We couldn\'t send your enquiry just now. Your details are still in the form – please try again in a moment.',
    ]);
}
