<?php
declare(strict_types=1);

/**
 * GET /app-api/v1/app-config.php
 * Public, non-secret settings the app reads at start-up.
 */

require __DIR__ . '/../lib/bootstrap.php';

$cfg = ftc_config();
if (!ftc_is_https()) {
    ftc_json(403, ['ok' => false, 'error' => 'https_required']);
}
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') {
    header('Allow: GET');
    ftc_json(405, ['ok' => false, 'error' => 'method_not_allowed']);
}

$contact = (string)($cfg['public_contact_email'] ?? '');
ftc_json(200, [
    'ok' => true,
    'api_version' => FTC_API_VERSION,
    'contact_email' => ftc_valid_email($contact) ? $contact : null,
]);
