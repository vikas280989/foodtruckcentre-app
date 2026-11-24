<?php
/**
 * WordPress admin: "App Enquiries" list + detail view, and Settings (recipient, contact email, limits, test email).
 * Administrators only (manage_options).
 */
if (!defined('ABSPATH')) {
    exit;
}

add_action('admin_menu', function () {
    add_menu_page('App Enquiries', 'App Enquiries', 'manage_options', 'ftc-app-enquiries', 'ftc_app_admin_list', 'dashicons-food', 26);
    add_submenu_page('ftc-app-enquiries', 'App Enquiries', 'All enquiries', 'manage_options', 'ftc-app-enquiries', 'ftc_app_admin_list');
    add_submenu_page('ftc-app-enquiries', 'App settings', 'Settings', 'manage_options', 'ftc-app-settings', 'ftc_app_admin_settings');
});

add_action('admin_init', function () {
    register_setting('ftc_app', 'ftc_app_recipient', ['sanitize_callback' => 'ftc_app_sanitize_email_option']);
    register_setting('ftc_app', 'ftc_app_contact_email', ['sanitize_callback' => 'ftc_app_sanitize_optional_email']);
    register_setting('ftc_app', 'ftc_app_rate_10min', ['sanitize_callback' => fn($v) => max(1, min(100, (int) $v))]);
    register_setting('ftc_app', 'ftc_app_rate_day', ['sanitize_callback' => fn($v) => max(1, min(1000, (int) $v))]);
    register_setting('ftc_app', 'ftc_app_min_fill', ['sanitize_callback' => fn($v) => max(0, min(60, (int) $v))]);
});

function ftc_app_sanitize_email_option($v): string
{
    $v = sanitize_email((string) $v);
    if (!ftc_app_valid_email($v)) {
        add_settings_error('ftc_app_recipient', 'bad', 'The enquiry recipient must be a valid email address. The previous value was kept.');
        return (string) get_option('ftc_app_recipient');
    }
    return $v;
}

function ftc_app_sanitize_optional_email($v): string
{
    $v = sanitize_email((string) $v);
    return ftc_app_valid_email($v) ? $v : '';
}

function ftc_app_admin_list(): void
{
    if (!current_user_can('manage_options')) {
        wp_die('Not allowed.');
    }
    global $wpdb;
    $table = ftc_app_table();

    if (!empty($_GET['view'])) {
        $row = $wpdb->get_row($wpdb->prepare("SELECT * FROM {$table} WHERE id = %d", (int) $_GET['view']), ARRAY_A);
        echo '<div class="wrap"><h1>Enquiry ' . esc_html($row['enquiry_ref'] ?? '') . '</h1>';
        echo '<p><a href="' . esc_url(admin_url('admin.php?page=ftc-app-enquiries')) . '">&larr; All enquiries</a></p>';
        if (!$row) {
            echo '<p>Not found.</p></div>';
            return;
        }
        echo '<table class="widefat striped" style="max-width:900px"><tbody>';
        $meta = [
            'Type' => ucfirst($row['type']), 'Status' => ftc_app_status_label($row),
            'Received (UTC)' => $row['created_at'], 'Delivered (UTC)' => (string) $row['sent_at'],
            'Delivery attempts' => (string) $row['attempts'],
        ];
        foreach ($meta as $k => $v) {
            echo '<tr><th style="width:280px">' . esc_html($k) . '</th><td>' . wp_kses_post($v) . '</td></tr>';
        }
        $sections = json_decode((string) $row['details'], true) ?: [];
        foreach ($sections as $title => $fields) {
            echo '<tr><th colspan="2" style="background:#f6f7f7"><strong>' . esc_html($title) . '</strong></th></tr>';
            foreach ((array) $fields as $label => $value) {
                echo '<tr><th>' . esc_html($label) . '</th><td>' . nl2br(esc_html((string) $value)) . '</td></tr>';
            }
        }
        echo '</tbody></table>';
        if (ftc_app_valid_email((string) $row['email'])) {
            echo '<p><a class="button button-primary" href="' . esc_url('mailto:' . $row['email'] . '?subject=' . rawurlencode('Re: ' . $row['subject'])) . '">Reply to ' . esc_html($row['name']) . '</a></p>';
        }
        echo '</div>';
        return;
    }

    $status = isset($_GET['status']) ? sanitize_key($_GET['status']) : '';
    $type = isset($_GET['type']) ? sanitize_key($_GET['type']) : '';
    $where = '1=1';
    $args = [];
    if (in_array($status, ['sent', 'failed', 'pending'], true)) {
        $where .= ' AND status = %s';
        $args[] = $status;
    }
    if (in_array($type, ['quote', 'contact'], true)) {
        $where .= ' AND type = %s';
        $args[] = $type;
    }
    $paged = max(1, (int) ($_GET['paged'] ?? 1));
    $per = 50;
    $sqlCount = "SELECT COUNT(*) FROM {$table} WHERE {$where}";
    $total = (int) ($args ? $wpdb->get_var($wpdb->prepare($sqlCount, $args)) : $wpdb->get_var($sqlCount));
    $sql = "SELECT id, enquiry_ref, type, status, name, email, phone, event_date, attempts, last_error, created_at, sent_at FROM {$table} WHERE {$where} ORDER BY id DESC LIMIT %d OFFSET %d";
    $rows = $wpdb->get_results($wpdb->prepare($sql, array_merge($args, [$per, ($paged - 1) * $per])), ARRAY_A);

    $base = admin_url('admin.php?page=ftc-app-enquiries');
    echo '<div class="wrap"><h1 class="wp-heading-inline">App Enquiries</h1>';
    echo '<p>Quote and contact enquiries sent from the Food Truck Centre Android app. Each one is also emailed to <strong>' . esc_html((string) get_option('ftc_app_recipient')) . '</strong>.</p>';
    $filters = ['' => 'All', 'quote' => 'Quotes', 'contact' => 'Contact messages'];
    echo '<ul class="subsubsub">';
    $links = [];
    foreach ($filters as $k => $label) {
        $links[] = '<li><a href="' . esc_url($k ? add_query_arg('type', $k, $base) : $base) . '"' . ($type === $k ? ' class="current"' : '') . '>' . esc_html($label) . '</a></li>';
    }
    $links[] = '<li><a href="' . esc_url(add_query_arg('status', 'failed', $base)) . '"' . ($status === 'failed' ? ' class="current"' : '') . '>Not delivered yet</a></li>';
    echo implode(' | ', $links) . '</ul>';
    echo '<table class="widefat striped" style="margin-top:12px"><thead><tr><th>Reference</th><th>Received</th><th>Type</th><th>Name</th><th>Email</th><th>Phone</th><th>Event date</th><th>Email status</th></tr></thead><tbody>';
    if (!$rows) {
        echo '<tr><td colspan="8">No enquiries yet.</td></tr>';
    }
    foreach ($rows as $r) {
        $url = add_query_arg('view', (int) $r['id'], $base);
        $local = get_date_from_gmt($r['created_at'], 'j M Y, g:i a');
        echo '<tr><td><a href="' . esc_url($url) . '"><strong>' . esc_html($r['enquiry_ref']) . '</strong></a></td>'
            . '<td>' . esc_html($local) . '</td><td>' . esc_html(ucfirst($r['type'])) . '</td>'
            . '<td>' . esc_html($r['name']) . '</td><td>' . esc_html($r['email']) . '</td><td>' . esc_html($r['phone']) . '</td>'
            . '<td>' . esc_html($r['event_date']) . '</td><td>' . wp_kses_post(ftc_app_status_label($r)) . '</td></tr>';
    }
    echo '</tbody></table>';
    $pages = (int) ceil($total / $per);
    if ($pages > 1) {
        echo '<p>Page ' . $paged . ' of ' . $pages . ' ';
        if ($paged > 1) echo '<a class="button" href="' . esc_url(add_query_arg('paged', $paged - 1)) . '">&larr; Newer</a> ';
        if ($paged < $pages) echo '<a class="button" href="' . esc_url(add_query_arg('paged', $paged + 1)) . '">Older &rarr;</a>';
        echo '</p>';
    }
    echo '</div>';
}

function ftc_app_status_label(array $r): string
{
    switch ($r['status']) {
        case 'sent':
            return '<span style="color:#1e8e5a">&#10003; Emailed</span>';
        case 'failed':
            return '<span style="color:#c62828">Not delivered yet</span> <small>(' . (int) $r['attempts'] . ' attempts, retrying automatically' . ($r['last_error'] ? ': ' . esc_html($r['last_error']) : '') . ')</small>';
        default:
            return 'Sending…';
    }
}

function ftc_app_admin_settings(): void
{
    if (!current_user_can('manage_options')) {
        wp_die('Not allowed.');
    }
    $notice = '';
    if (isset($_POST['ftc_app_action']) && check_admin_referer('ftc_app_tools')) {
        if ($_POST['ftc_app_action'] === 'test') {
            $to = (string) get_option('ftc_app_recipient');
            $ok = wp_mail($to, 'Food Truck Centre app – test email', '<p>This is a test from the Food Truck Centre App API plugin. If you can read this, app enquiries will be delivered.</p>', ['Content-Type: text/html; charset=UTF-8']);
            $notice = $ok
                ? '<div class="notice notice-success"><p>Test email accepted for delivery to ' . esc_html($to) . '. Check the inbox (and spam folder).</p></div>'
                : '<div class="notice notice-error"><p>WordPress could not send the test email. Check your SMTP plugin settings.</p></div>';
        } elseif ($_POST['ftc_app_action'] === 'retry') {
            ftc_app_retry_failed();
            $notice = '<div class="notice notice-info"><p>Retried undelivered enquiries. See App Enquiries for the result.</p></div>';
        }
    }
    $endpoint = home_url('/app-api/v1/app-config.php');
    ?>
    <div class="wrap">
        <h1>Food Truck Centre app – settings</h1>
        <?php echo $notice; settings_errors(); ?>
        <form method="post" action="options.php">
            <?php settings_fields('ftc_app'); ?>
            <table class="form-table" role="presentation">
                <tr><th scope="row"><label for="ftc_app_recipient">Send every enquiry to</label></th>
                    <td><input name="ftc_app_recipient" id="ftc_app_recipient" type="email" class="regular-text" value="<?php echo esc_attr((string) get_option('ftc_app_recipient')); ?>" required>
                    <p class="description">All quote and contact enquiries from the app are emailed here.</p></td></tr>
                <tr><th scope="row"><label for="ftc_app_contact_email">Email shown in the app</label></th>
                    <td><input name="ftc_app_contact_email" id="ftc_app_contact_email" type="email" class="regular-text" value="<?php echo esc_attr((string) get_option('ftc_app_contact_email')); ?>">
                    <p class="description">Shown on the app's Contact screen. Leave empty to hide it.</p></td></tr>
                <tr><th scope="row">Spam protection</th>
                    <td><label>Max <input name="ftc_app_rate_10min" type="number" min="1" max="100" class="small-text" value="<?php echo (int) get_option('ftc_app_rate_10min', 5); ?>"> per 10 minutes</label> &nbsp;
                        <label>and <input name="ftc_app_rate_day" type="number" min="1" max="1000" class="small-text" value="<?php echo (int) get_option('ftc_app_rate_day', 20); ?>"> per day, per connection</label><br>
                        <label>Minimum time to fill a form: <input name="ftc_app_min_fill" type="number" min="0" max="60" class="small-text" value="<?php echo (int) get_option('ftc_app_min_fill', 4); ?>"> seconds</label></td></tr>
            </table>
            <?php submit_button(); ?>
        </form>
        <h2>Tools</h2>
        <form method="post" style="display:inline-block;margin-right:8px">
            <?php wp_nonce_field('ftc_app_tools'); ?><input type="hidden" name="ftc_app_action" value="test">
            <?php submit_button('Send a test email', 'secondary', 'submit', false); ?>
        </form>
        <form method="post" style="display:inline-block">
            <?php wp_nonce_field('ftc_app_tools'); ?><input type="hidden" name="ftc_app_action" value="retry">
            <?php submit_button('Retry undelivered enquiries now', 'secondary', 'submit', false); ?>
        </form>
        <p class="description" style="margin-top:16px">App connection check: <a href="<?php echo esc_url($endpoint); ?>" target="_blank" rel="noopener"><?php echo esc_html($endpoint); ?></a> should show <code>{"ok":true,…}</code>.</p>
    </div>
    <?php
}
