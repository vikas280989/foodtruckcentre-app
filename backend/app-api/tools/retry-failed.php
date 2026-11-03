<?php
declare(strict_types=1);

/**
 * Re-sends enquiries that could not be delivered at the time they were submitted.
 * Run from the command line or a Hostinger cron job, e.g. every 15 minutes:
 *   php /home/<you>/public_html/app-api/tools/retry-failed.php
 */

if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}

require __DIR__ . '/../lib/bootstrap.php';
require __DIR__ . '/../lib/mailer.php';

ftc_config();
$files = glob(ftc_storage('failed') . '/*.json') ?: [];
$sent = 0;
$left = 0;
foreach ($files as $file) {
    $msg = json_decode((string)file_get_contents($file), true);
    if (!is_array($msg) || empty($msg['subject'])) {
        continue;
    }
    if (ftc_send_mail($msg)) {
        unlink($file);
        $sent++;
    } else {
        $left++;
    }
}
echo "Re-sent: {$sent}. Still waiting: {$left}.\n";
