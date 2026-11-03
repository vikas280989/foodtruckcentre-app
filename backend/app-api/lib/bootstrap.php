<?php
declare(strict_types=1);

/**
 * Shared bootstrap for the Food Truck Centre app API.
 * Loads configuration, sets security headers and provides helpers.
 */

const FTC_API_VERSION = '1.0.0';

// Never leak PHP errors to clients.
ini_set('display_errors', '0');
error_reporting(E_ALL);

function ftc_private_dir(): string
{
    $candidates = [];
    $env = getenv('FTC_PRIVATE_DIR');
    if ($env) {
        $candidates[] = $env;
    }
    // Recommended: /home/<user>/ftc-app-private  (app-api lives in /home/<user>/public_html/app-api)
    $candidates[] = dirname(__DIR__, 3) . '/ftc-app-private';
    // Fallback: app-api/private (protected by .htaccess)
    $candidates[] = dirname(__DIR__) . '/private';
    foreach ($candidates as $dir) {
        if (is_file($dir . '/config.php')) {
            return $dir;
        }
    }
    return '';
}

function ftc_config(): array
{
    static $config = null;
    if ($config !== null) {
        return $config;
    }
    $dir = ftc_private_dir();
    if ($dir === '') {
        error_log('[ftc-app-api] config.php not found');
        ftc_json(503, ['ok' => false, 'error' => 'not_configured', 'message' => 'The service is not available right now. Please try again later.']);
    }
    $loaded = require $dir . '/config.php';
    if (!is_array($loaded) || empty($loaded['recipient_email']) || !filter_var($loaded['recipient_email'], FILTER_VALIDATE_EMAIL)) {
        error_log('[ftc-app-api] config.php invalid (recipient_email)');
        ftc_json(503, ['ok' => false, 'error' => 'not_configured', 'message' => 'The service is not available right now. Please try again later.']);
    }
    $loaded['storage_dir'] = $loaded['storage_dir'] ?? ($dir . '/storage');
    $loaded['rate_limit'] = ($loaded['rate_limit'] ?? []) + ['per_10_minutes' => 5, 'per_day' => 20];
    $loaded['min_fill_seconds'] = (int)($loaded['min_fill_seconds'] ?? 4);
    $loaded['max_body_bytes'] = (int)($loaded['max_body_bytes'] ?? 16384);
    $config = $loaded;
    return $config;
}

function ftc_headers(): void
{
    header('Content-Type: application/json; charset=utf-8');
    header('X-Content-Type-Options: nosniff');
    header('X-Frame-Options: DENY');
    header('Referrer-Policy: no-referrer');
    header("Content-Security-Policy: default-src 'none'; frame-ancestors 'none'");
    header('Cache-Control: no-store');
    header('Strict-Transport-Security: max-age=31536000');
}

/** Send a JSON response and stop. */
function ftc_json(int $status, array $body): never
{
    if (!headers_sent()) {
        http_response_code($status);
        ftc_headers();
    }
    echo json_encode($body, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function ftc_is_https(): bool
{
    if (PHP_SAPI === 'cli-server' && getenv('FTC_ALLOW_HTTP_FOR_TESTS') === '1') {
        return true; // local automated tests only
    }
    if (!empty($_SERVER['HTTPS']) && strtolower((string)$_SERVER['HTTPS']) !== 'off') {
        return true;
    }
    if (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https') {
        return true;
    }
    return (int)($_SERVER['SERVER_PORT'] ?? 0) === 443;
}

function ftc_client_ip(): string
{
    // Only trust REMOTE_ADDR; proxy headers can be forged by clients.
    return (string)($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0');
}

function ftc_storage(string $sub): string
{
    $base = rtrim(ftc_config()['storage_dir'], '/');
    $dir = $base . '/' . $sub;
    if (!is_dir($dir)) {
        @mkdir($dir, 0700, true);
        @file_put_contents($base . '/.htaccess', "Require all denied\n");
    }
    return $dir;
}

/**
 * Common checks for POST endpoints. Returns decoded JSON body.
 */
function ftc_require_post_json(): array
{
    if (!ftc_is_https()) {
        ftc_json(403, ['ok' => false, 'error' => 'https_required', 'message' => 'A secure connection is required.']);
    }
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        header('Allow: POST');
        ftc_json(405, ['ok' => false, 'error' => 'method_not_allowed', 'message' => 'Method not allowed.']);
    }
    $type = strtolower((string)($_SERVER['CONTENT_TYPE'] ?? ''));
    if (!str_starts_with($type, 'application/json')) {
        ftc_json(415, ['ok' => false, 'error' => 'unsupported_media_type', 'message' => 'Unsupported request.']);
    }
    $max = ftc_config()['max_body_bytes'];
    $declared = (int)($_SERVER['CONTENT_LENGTH'] ?? 0);
    if ($declared > $max) {
        ftc_json(413, ['ok' => false, 'error' => 'too_large', 'message' => 'Your message is too long.']);
    }
    $raw = file_get_contents('php://input', false, null, 0, $max + 1);
    if ($raw === false || strlen($raw) > $max) {
        ftc_json(413, ['ok' => false, 'error' => 'too_large', 'message' => 'Your message is too long.']);
    }
    $data = json_decode($raw, true, 8);
    if (!is_array($data) || array_is_list($data)) {
        ftc_json(400, ['ok' => false, 'error' => 'bad_request', 'message' => 'Invalid request.']);
    }
    return $data;
}

/**
 * File-based sliding-window rate limiter keyed by a hash of the client IP.
 */
function ftc_rate_limit(string $bucket): void
{
    $cfg = ftc_config()['rate_limit'];
    $ip = ftc_client_ip();
    $key = hash('sha256', $bucket . '|' . $ip);
    $file = ftc_storage('ratelimit') . '/' . $key . '.json';
    $now = time();

    $fh = fopen($file, 'c+');
    if ($fh === false) {
        error_log('[ftc-app-api] rate limit storage not writable');
        return; // fail open on storage errors, but log it
    }
    flock($fh, LOCK_EX);
    $raw = stream_get_contents($fh);
    $hits = json_decode($raw ?: '[]', true);
    $hits = is_array($hits) ? array_values(array_filter($hits, fn($t) => is_int($t) && $t > $now - 86400)) : [];
    $recent = count(array_filter($hits, fn($t) => $t > $now - 600));
    if ($recent >= (int)$cfg['per_10_minutes'] || count($hits) >= (int)$cfg['per_day']) {
        flock($fh, LOCK_UN);
        fclose($fh);
        header('Retry-After: 600');
        ftc_json(429, ['ok' => false, 'error' => 'rate_limited', 'message' => 'Too many requests. Please wait a few minutes and try again.']);
    }
    $hits[] = $now;
    ftruncate($fh, 0);
    rewind($fh);
    fwrite($fh, json_encode($hits));
    fflush($fh);
    flock($fh, LOCK_UN);
    fclose($fh);
    @chmod($file, 0600);

    // Occasional clean-up of stale counters.
    if (random_int(1, 50) === 1) {
        foreach (glob(ftc_storage('ratelimit') . '/*.json') ?: [] as $f) {
            if (filemtime($f) < $now - 86400) {
                @unlink($f);
            }
        }
    }
}

/** Remove control characters (except newlines in multi-line fields) and trim. */
function ftc_clean(mixed $value, int $maxLen, bool $multiline = false): string
{
    if (!is_string($value) && !is_int($value) && !is_float($value)) {
        return '';
    }
    $s = (string)$value;
    if (!mb_check_encoding($s, 'UTF-8')) {
        return '';
    }
    $s = str_replace(["\r\n", "\r"], "\n", $s);
    $pattern = $multiline ? '/[\x00-\x09\x0B-\x1F\x7F]/u' : '/[\x00-\x1F\x7F]/u';
    $s = preg_replace($pattern, '', $s) ?? '';
    $s = trim($s);
    if (mb_strlen($s) > $maxLen) {
        $s = mb_substr($s, 0, $maxLen);
    }
    return $s;
}

function ftc_valid_email(string $email): bool
{
    return strlen($email) <= 254
        && filter_var($email, FILTER_VALIDATE_EMAIL) !== false
        && !preg_match('/[\r\n,;<>]/', $email);
}

function ftc_valid_phone(string $phone): bool
{
    if (!preg_match('/^[0-9+()\-\s]{8,20}$/', $phone)) {
        return false;
    }
    $digits = preg_replace('/\D/', '', $phone) ?? '';
    return strlen($digits) >= 8 && strlen($digits) <= 15;
}

function ftc_link_count(string $text): int
{
    return preg_match_all('~(https?://|www\.)~i', $text);
}

/**
 * Bot checks shared by all forms: honeypot and minimum fill time.
 * Returns true when the request looks automated.
 */
function ftc_looks_like_bot(array $data): bool
{
    if (ftc_clean($data['website'] ?? '', 200) !== '') {
        return true; // honeypot field must stay empty
    }
    $elapsed = $data['elapsed_seconds'] ?? null;
    if (!is_int($elapsed) && !(is_numeric($elapsed))) {
        return true;
    }
    return (int)$elapsed < ftc_config()['min_fill_seconds'];
}

/**
 * Idempotency: the app sends a random UUID per form. A repeat of an already-delivered
 * submission returns the original success without sending a second email.
 */
function ftc_idempotency_key(array $data): string
{
    $key = (string)($data['submission_id'] ?? '');
    if (!preg_match('/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i', $key)) {
        ftc_json(400, ['ok' => false, 'error' => 'bad_request', 'message' => 'Invalid request. Please restart the app and try again.']);
    }
    return strtolower($key);
}

function ftc_idempotency_lookup(string $key): ?array
{
    $file = ftc_storage('delivered') . '/' . hash('sha256', $key) . '.json';
    if (!is_file($file)) {
        return null;
    }
    $rec = json_decode((string)file_get_contents($file), true);
    return is_array($rec) ? $rec : null;
}

function ftc_idempotency_store(string $key, array $record): void
{
    $dir = ftc_storage('delivered');
    $file = $dir . '/' . hash('sha256', $key) . '.json';
    file_put_contents($file, json_encode($record), LOCK_EX);
    @chmod($file, 0600);
    if (random_int(1, 50) === 1) {
        foreach (glob($dir . '/*.json') ?: [] as $f) {
            if (filemtime($f) < time() - 7 * 86400) {
                @unlink($f);
            }
        }
    }
}

/** Content-based duplicate check: identical payload delivered in the last 30 minutes. */
function ftc_recent_duplicate(string $fingerprint): ?array
{
    $file = ftc_storage('fingerprints') . '/' . $fingerprint . '.json';
    if (is_file($file) && filemtime($file) > time() - 1800) {
        $rec = json_decode((string)file_get_contents($file), true);
        return is_array($rec) ? $rec : null;
    }
    return null;
}

function ftc_remember_fingerprint(string $fingerprint, array $record): void
{
    $dir = ftc_storage('fingerprints');
    file_put_contents($dir . '/' . $fingerprint . '.json', json_encode($record), LOCK_EX);
    @chmod($dir . '/' . $fingerprint . '.json', 0600);
    if (random_int(1, 50) === 1) {
        foreach (glob($dir . '/*.json') ?: [] as $f) {
            if (filemtime($f) < time() - 86400) {
                @unlink($f);
            }
        }
    }
}

/** Keep an undelivered enquiry so it can be re-sent later with tools/retry-failed.php. */
function ftc_save_failed(string $key, array $message): void
{
    $file = ftc_storage('failed') . '/' . hash('sha256', $key) . '.json';
    file_put_contents($file, json_encode($message, JSON_UNESCAPED_UNICODE), LOCK_EX);
    @chmod($file, 0600);
}

function ftc_clear_failed(string $key): void
{
    $file = ftc_storage('failed') . '/' . hash('sha256', $key) . '.json';
    if (is_file($file)) {
        @unlink($file);
    }
}

function ftc_new_enquiry_id(): string
{
    return 'FTC-' . gmdate('ymd') . '-' . strtoupper(bin2hex(random_bytes(3)));
}

function ftc_h(string $s): string
{
    return htmlspecialchars($s, ENT_QUOTES | ENT_SUBSTITUTE | ENT_HTML5, 'UTF-8');
}
