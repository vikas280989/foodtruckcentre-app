<?php
if (!defined('ABSPATH')) {
    exit;
}

/** Which app sent the request ("iOS" or "Android"), from the app's User-Agent. */
function ftc_app_platform(): string
{
    $ua = (string) ($_SERVER['HTTP_USER_AGENT'] ?? '');
    return str_contains($ua, '(iOS)') ? 'iOS' : 'Android';
}

/** Send a JSON response in the exact format the apps expect, and stop. */
function ftc_app_json(int $status, array $body): void
{
    if (!headers_sent()) {
        status_header($status);
        nocache_headers();
        header('Content-Type: application/json; charset=utf-8');
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('Referrer-Policy: no-referrer');
        header("Content-Security-Policy: default-src 'none'; frame-ancestors 'none'");
        header('X-LiteSpeed-Cache-Control: no-cache'); // Hostinger LiteSpeed: never cache API answers
    }
    echo wp_json_encode($body, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function ftc_app_is_https(): bool
{
    if (defined('FTC_APP_ALLOW_HTTP_FOR_TESTS') && FTC_APP_ALLOW_HTTP_FOR_TESTS) {
        return true; // local automated tests only – never define this on a live site
    }
    return is_ssl() || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https');
}

function ftc_app_client_ip(): string
{
    // Only REMOTE_ADDR is trusted; proxy headers can be forged. Filterable for sites behind a known proxy.
    return (string) apply_filters('ftc_app_client_ip', (string) ($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0'));
}

/** Remove control characters (keep newlines in multi-line fields), validate UTF-8, trim and cap length. */
function ftc_app_clean($value, int $maxLen, bool $multiline = false): string
{
    if (!is_string($value) && !is_int($value) && !is_float($value)) {
        return '';
    }
    $s = (string) $value;
    if (!mb_check_encoding($s, 'UTF-8')) {
        return '';
    }
    $s = str_replace(["\r\n", "\r"], "\n", $s);
    $pattern = $multiline ? '/[\x00-\x09\x0B-\x1F\x7F]/u' : '/[\x00-\x1F\x7F]/u';
    $s = trim(preg_replace($pattern, '', $s) ?? '');
    if (mb_strlen($s) > $maxLen) {
        $s = mb_substr($s, 0, $maxLen);
    }
    return $s;
}

function ftc_app_valid_email(string $email): bool
{
    return strlen($email) <= 254
        && filter_var($email, FILTER_VALIDATE_EMAIL) !== false
        && !preg_match('/[\r\n,;<>"]/', $email);
}

function ftc_app_valid_phone(string $phone): bool
{
    if (!preg_match('/^[0-9+()\-\s]{8,20}$/', $phone)) {
        return false;
    }
    $digits = preg_replace('/\D/', '', $phone) ?? '';
    return strlen($digits) >= 8 && strlen($digits) <= 15;
}

function ftc_app_link_count(string $text): int
{
    return (int) preg_match_all('~(https?://|www\.)~i', $text);
}

function ftc_app_h(string $s): string
{
    return htmlspecialchars($s, ENT_QUOTES | ENT_SUBSTITUTE | ENT_HTML5, 'UTF-8');
}

function ftc_app_new_enquiry_ref(): string
{
    // Letters, digits and dashes only: the app puts this in a screen route.
    return 'FTC-' . gmdate('ymd') . '-' . strtoupper(bin2hex(random_bytes(3)));
}

/**
 * Sliding-window rate limit per client IP, kept in transients (no extra tables).
 * Returns true when the request may continue.
 */
function ftc_app_rate_limit_ok(string $bucket = 'submit', ?int $per10 = null, ?int $perDay = null): bool
{
    $per10 = max(1, $per10 ?? (int) get_option('ftc_app_rate_10min', 5));
    $perDay = max(1, $perDay ?? (int) get_option('ftc_app_rate_day', 20));
    $key = 'ftc_app_rl_' . substr(hash('sha256', $bucket . '|' . ftc_app_client_ip() . '|' . wp_salt('nonce')), 0, 32);
    $now = time();
    $hits = get_transient($key);
    $hits = is_array($hits) ? array_values(array_filter($hits, fn($t) => is_int($t) && $t > $now - DAY_IN_SECONDS)) : [];
    $recent = count(array_filter($hits, fn($t) => $t > $now - 600));
    if ($recent >= $per10 || count($hits) >= $perDay) {
        return false;
    }
    $hits[] = $now;
    set_transient($key, $hits, DAY_IN_SECONDS);
    return true;
}
