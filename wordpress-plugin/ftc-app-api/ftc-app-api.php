<?php
/**
 * Plugin Name:       Food Truck Centre App API
 * Description:       Receives quote and contact enquiries from the Food Truck Centre Android app, emails them through WordPress (your existing SMTP setup) and keeps a copy under "App Enquiries". No payments.
 * Version:           1.0.0
 * Requires at least: 6.0
 * Requires PHP:      8.1
 * Author:            Food Truck Centre
 * License:           GPL-2.0-or-later
 * Text Domain:       ftc-app-api
 */

if (!defined('ABSPATH')) {
    exit;
}

define('FTC_APP_VERSION', '1.0.0');
define('FTC_APP_DB_VERSION', '1');
define('FTC_APP_DIR', __DIR__);

require_once FTC_APP_DIR . '/includes/helpers.php';
require_once FTC_APP_DIR . '/includes/validation.php';
require_once FTC_APP_DIR . '/includes/mail.php';
require_once FTC_APP_DIR . '/includes/store.php';
require_once FTC_APP_DIR . '/includes/api.php';
if (is_admin()) {
    require_once FTC_APP_DIR . '/includes/admin.php';
}

register_activation_hook(__FILE__, 'ftc_app_activate');
register_deactivation_hook(__FILE__, 'ftc_app_deactivate');

function ftc_app_activate(): void
{
    ftc_app_install_table();
    add_option('ftc_app_recipient', 'vikas.kumar617@gmail.com');
    add_option('ftc_app_contact_email', 'vikas.kumar617@gmail.com');
    add_option('ftc_app_rate_10min', 5);
    add_option('ftc_app_rate_day', 20);
    add_option('ftc_app_min_fill', 4);
    if (!wp_next_scheduled('ftc_app_retry_failed')) {
        wp_schedule_event(time() + 300, 'ftc_app_15min', 'ftc_app_retry_failed');
    }
}

function ftc_app_deactivate(): void
{
    wp_clear_scheduled_hook('ftc_app_retry_failed');
}

// Upgrade the table if the plugin is updated without re-activation.
add_action('plugins_loaded', function () {
    if (get_option('ftc_app_db_version') !== FTC_APP_DB_VERSION) {
        ftc_app_install_table();
    }
});

add_filter('cron_schedules', function (array $s): array {
    $s['ftc_app_15min'] = ['interval' => 900, 'display' => 'Every 15 minutes (FTC App)'];
    return $s;
});
add_action('ftc_app_retry_failed', 'ftc_app_retry_failed');

// Handle the app's endpoints as early as possible, before themes or page caching get involved.
add_action('init', 'ftc_app_maybe_handle_request', 0);
add_action('rest_api_init', 'ftc_app_register_rest_routes');
