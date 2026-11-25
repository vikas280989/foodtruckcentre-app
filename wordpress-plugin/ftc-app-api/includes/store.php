<?php
/**
 * Enquiry storage in the WordPress database (one table, created on activation).
 */
if (!defined('ABSPATH')) {
    exit;
}

function ftc_app_table(): string
{
    global $wpdb;
    return $wpdb->prefix . 'ftc_app_enquiries';
}

function ftc_app_tokens_table(): string
{
    global $wpdb;
    return $wpdb->prefix . 'ftc_app_tokens';
}

/** Customer-visible progress of an enquiry, set by the team in App Enquiries. */
function ftc_app_stages(): array
{
    return ['new' => 'New', 'contacted' => 'Contacted', 'quoted' => 'Quoted', 'confirmed' => 'Confirmed', 'closed' => 'Closed'];
}

function ftc_app_install_table(): void
{
    global $wpdb;
    require_once ABSPATH . 'wp-admin/includes/upgrade.php';
    $table = ftc_app_table();
    $charset = $wpdb->get_charset_collate();
    dbDelta("CREATE TABLE {$table} (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  enquiry_ref varchar(32) NOT NULL,
  type varchar(16) NOT NULL,
  user_id bigint(20) unsigned NOT NULL DEFAULT 0,
  stage varchar(20) NOT NULL DEFAULT 'new',
  submission_hash char(64) NOT NULL,
  fingerprint char(64) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'pending',
  name varchar(100) NOT NULL DEFAULT '',
  email varchar(254) NOT NULL DEFAULT '',
  phone varchar(20) NOT NULL DEFAULT '',
  event_date varchar(10) NOT NULL DEFAULT '',
  subject varchar(255) NOT NULL DEFAULT '',
  details longtext NOT NULL,
  body_html longtext NOT NULL,
  body_text longtext NOT NULL,
  attempts smallint(5) unsigned NOT NULL DEFAULT 0,
  last_error varchar(255) NOT NULL DEFAULT '',
  created_at datetime NOT NULL,
  sent_at datetime NULL DEFAULT NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY submission_hash (submission_hash),
  KEY fingerprint (fingerprint),
  KEY status (status),
  KEY created_at (created_at),
  KEY user_id (user_id)
) {$charset};");
    $tokens = ftc_app_tokens_table();
    dbDelta("CREATE TABLE {$tokens} (
  token_hash char(64) NOT NULL,
  user_id bigint(20) unsigned NOT NULL,
  created_at datetime NOT NULL,
  expires_at datetime NOT NULL,
  last_used datetime NULL DEFAULT NULL,
  PRIMARY KEY  (token_hash),
  KEY user_id (user_id)
) {$charset};");
    update_option('ftc_app_db_version', FTC_APP_DB_VERSION, false);
}

function ftc_app_find_by_submission(string $hash): ?array
{
    global $wpdb;
    $row = $wpdb->get_row($wpdb->prepare('SELECT * FROM ' . ftc_app_table() . ' WHERE submission_hash = %s', $hash), ARRAY_A);
    return $row ?: null;
}

/** A delivered enquiry with identical content in the last 30 minutes. */
function ftc_app_find_recent_duplicate(string $fingerprint): ?array
{
    global $wpdb;
    $since = gmdate('Y-m-d H:i:s', time() - 1800);
    $row = $wpdb->get_row($wpdb->prepare(
        'SELECT * FROM ' . ftc_app_table() . " WHERE fingerprint = %s AND status = 'sent' AND created_at >= %s ORDER BY id DESC LIMIT 1",
        $fingerprint, $since
    ), ARRAY_A);
    return $row ?: null;
}

/**
 * Save (or, for a retry of a failed submission, refresh) the enquiry before sending,
 * so nothing is lost even if the mail server is down. Returns the stored row.
 */
function ftc_app_save_enquiry(string $submissionHash, array $e, ?array $existing, int $userId = 0): array
{
    global $wpdb;
    $ref = $existing['enquiry_ref'] ?? ftc_app_new_enquiry_ref();
    $data = [
        'enquiry_ref' => $ref,
        'type' => $e['type'],
        'user_id' => $userId,
        'submission_hash' => $submissionHash,
        'fingerprint' => $e['fingerprint'],
        'status' => 'pending',
        'name' => $e['name'],
        'email' => $e['email'],
        'phone' => $e['phone'],
        'event_date' => $e['event_date'],
        'subject' => mb_substr($e['subject'], 0, 255),
        'details' => wp_json_encode($e['sections'], JSON_UNESCAPED_UNICODE),
        'body_html' => ftc_app_email_html($e['heading'], $e['intro'], $e['sections'], $ref),
        'body_text' => ftc_app_email_text($e['heading'], $e['sections'], $ref),
    ];
    if ($existing) {
        $wpdb->update(ftc_app_table(), $data, ['id' => (int) $existing['id']]);
        $data['id'] = (int) $existing['id'];
        $data['attempts'] = (int) $existing['attempts'];
    } else {
        $data['created_at'] = current_time('mysql', true);
        $data['attempts'] = 0;
        $ok = $wpdb->insert(ftc_app_table(), $data);
        if (!$ok) {
            // Another request with the same submission ID won the race: use that one.
            $other = ftc_app_find_by_submission($submissionHash);
            if ($other) {
                return $other;
            }
            error_log('[ftc-app-api] could not store enquiry: ' . $wpdb->last_error);
            ftc_app_json(503, ['ok' => false, 'error' => 'storage', 'message' => 'The service is not available right now. Please try again later.']);
        }
        $data['id'] = (int) $wpdb->insert_id;
    }
    return $data;
}

function ftc_app_mark(array $row, bool $sent, string $error): void
{
    global $wpdb;
    $wpdb->update(ftc_app_table(), [
        'status' => $sent ? 'sent' : 'failed',
        'attempts' => (int) $row['attempts'] + 1,
        'last_error' => $sent ? '' : mb_substr($error, 0, 255),
        'sent_at' => $sent ? current_time('mysql', true) : null,
    ], ['id' => (int) $row['id']]);
}

/** WP-Cron (every 15 minutes): re-send enquiries that could not be delivered, up to 20 attempts. */
function ftc_app_retry_failed(): void
{
    global $wpdb;
    $stale = gmdate('Y-m-d H:i:s', time() - 600); // "pending" for 10+ minutes = the request died mid-send
    $rows = $wpdb->get_results($wpdb->prepare(
        'SELECT * FROM ' . ftc_app_table() . " WHERE attempts < 20 AND (status = 'failed' OR (status = 'pending' AND created_at < %s)) ORDER BY id ASC LIMIT 20",
        $stale
    ), ARRAY_A);
    foreach ($rows ?: [] as $row) {
        [$sent, $error] = ftc_app_send($row);
        ftc_app_mark($row, $sent, $error);
    }
}
