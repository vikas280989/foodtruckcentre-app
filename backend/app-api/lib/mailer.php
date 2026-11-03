<?php
declare(strict_types=1);

require_once __DIR__ . '/../vendor/phpmailer/Exception.php';
require_once __DIR__ . '/../vendor/phpmailer/PHPMailer.php';
require_once __DIR__ . '/../vendor/phpmailer/SMTP.php';

use PHPMailer\PHPMailer\PHPMailer;

/**
 * Sends one message to the configured recipient.
 * $message = ['subject' => ..., 'html' => ..., 'text' => ..., 'reply_to_email' => ..., 'reply_to_name' => ...]
 * Returns true only when the mail server accepted the message.
 */
function ftc_send_mail(array $message): bool
{
    $cfg = ftc_config();
    $mail = $cfg['mail'] ?? [];
    $driver = $mail['driver'] ?? 'smtp';

    if ($driver === 'log') {
        // Testing only: write the message to storage/outbox instead of sending.
        if (getenv('FTC_FORCE_MAIL_FAILURE') === '1' || is_file(ftc_storage('outbox') . '/FAIL')) {
            return false;
        }
        $file = ftc_storage('outbox') . '/' . gmdate('Ymd-His') . '-' . bin2hex(random_bytes(3)) . '.json';
        file_put_contents($file, json_encode($message + ['to' => $cfg['recipient_email']], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        return true;
    }

    try {
        $m = new PHPMailer(true);
        $m->CharSet = PHPMailer::CHARSET_UTF8;
        if ($driver === 'smtp') {
            $m->isSMTP();
            $m->Host = (string)$mail['host'];
            $m->Port = (int)$mail['port'];
            $m->SMTPAuth = true;
            $m->Username = (string)$mail['username'];
            $m->Password = (string)$mail['password'];
            $m->SMTPSecure = ($mail['encryption'] ?? 'ssl') === 'tls' ? PHPMailer::ENCRYPTION_STARTTLS : PHPMailer::ENCRYPTION_SMTPS;
            $m->Timeout = (int)($mail['timeout'] ?? 20);
            $m->SMTPAutoTLS = true;
        } else {
            $m->isMail();
        }
        $m->setFrom((string)$mail['from_email'], (string)($mail['from_name'] ?? 'Food Truck Centre App'));
        $m->addAddress((string)$cfg['recipient_email'], (string)($cfg['recipient_name'] ?? ''));
        if (!empty($message['reply_to_email']) && ftc_valid_email($message['reply_to_email'])) {
            $m->addReplyTo($message['reply_to_email'], (string)($message['reply_to_name'] ?? ''));
        }
        $m->Subject = $message['subject'];
        $m->isHTML(true);
        $m->Body = $message['html'];
        $m->AltBody = $message['text'];
        $m->send();
        return true;
    } catch (\Throwable $e) {
        // Log without personal data or credentials.
        error_log('[ftc-app-api] mail send failed: ' . get_class($e));
        return false;
    }
}

/**
 * Build a branded HTML email from labelled rows.
 * $sections = ['Section title' => ['Label' => 'value', ...], ...]
 */
function ftc_email_html(string $heading, string $intro, array $sections, string $enquiryId): string
{
    $rows = '';
    foreach ($sections as $title => $fields) {
        $rows .= '<tr><td colspan="2" style="padding:22px 0 8px;font:700 13px/1.3 Arial,sans-serif;letter-spacing:.08em;text-transform:uppercase;color:#D62828;border-bottom:2px solid #F4B400;">' . ftc_h($title) . '</td></tr>';
        foreach ($fields as $label => $value) {
            $display = $value === '' ? '<span style="color:#94a3b8">Not provided</span>' : nl2br(ftc_h($value));
            $rows .= '<tr>'
                . '<td style="padding:10px 12px 10px 0;width:42%;vertical-align:top;font:600 14px/1.4 Arial,sans-serif;color:#0A1F5C;border-bottom:1px solid #eef0f4;">' . ftc_h($label) . '</td>'
                . '<td style="padding:10px 0;vertical-align:top;font:400 14px/1.5 Arial,sans-serif;color:#1e293b;border-bottom:1px solid #eef0f4;">' . $display . '</td>'
                . '</tr>';
        }
    }
    $received = gmdate('D j M Y, H:i') . ' UTC';

    return '<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"></head>'
        . '<body style="margin:0;padding:0;background:#FFF8F1;">'
        . '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#FFF8F1;padding:24px 12px;"><tr><td align="center">'
        . '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:640px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 18px rgba(10,31,92,.08);">'
        . '<tr><td style="background:#0A1F5C;padding:26px 28px;">'
        . '<div style="font:700 12px/1 Arial,sans-serif;letter-spacing:.14em;color:#F4B400;text-transform:uppercase;">Food Truck Centre App</div>'
        . '<div style="font:700 22px/1.3 Arial,sans-serif;color:#ffffff;margin-top:10px;">' . ftc_h($heading) . '</div>'
        . '</td></tr>'
        . '<tr><td style="height:5px;background:#D62828;"></td></tr>'
        . '<tr><td style="padding:22px 28px 6px;font:400 15px/1.6 Arial,sans-serif;color:#334155;">' . ftc_h($intro) . '</td></tr>'
        . '<tr><td style="padding:0 28px 24px;"><table role="presentation" width="100%" cellpadding="0" cellspacing="0">' . $rows . '</table></td></tr>'
        . '<tr><td style="padding:16px 28px;background:#f7f9fb;font:400 12px/1.6 Arial,sans-serif;color:#64748b;">'
        . 'Enquiry ID: <strong>' . ftc_h($enquiryId) . '</strong><br>Received: ' . ftc_h($received) . '<br>'
        . 'Sent from the Food Truck Centre Android app. Reply to this email to respond directly to the customer.'
        . '</td></tr></table></td></tr></table></body></html>';
}

function ftc_email_text(string $heading, array $sections, string $enquiryId): string
{
    $out = $heading . "\n" . str_repeat('=', mb_strlen($heading)) . "\n";
    foreach ($sections as $title => $fields) {
        $out .= "\n" . strtoupper($title) . "\n";
        foreach ($fields as $label => $value) {
            $out .= $label . ': ' . ($value === '' ? 'Not provided' : $value) . "\n";
        }
    }
    return $out . "\nEnquiry ID: " . $enquiryId . "\n";
}

/**
 * Deliver (with duplicate protection and failure preservation) and respond to the app.
 */
function ftc_deliver_and_respond(string $idemKey, string $fingerprint, array $message, string $enquiryId): never
{
    $previous = ftc_idempotency_lookup($idemKey) ?? ftc_recent_duplicate($fingerprint);
    if ($previous !== null) {
        ftc_json(200, ['ok' => true, 'enquiry_id' => $previous['enquiry_id'], 'duplicate' => true]);
    }

    if (ftc_send_mail($message)) {
        $record = ['enquiry_id' => $enquiryId, 'at' => time()];
        ftc_idempotency_store($idemKey, $record);
        ftc_remember_fingerprint($fingerprint, $record);
        ftc_clear_failed($idemKey);
        ftc_json(200, ['ok' => true, 'enquiry_id' => $enquiryId]);
    }

    ftc_save_failed($idemKey, $message + ['enquiry_id' => $enquiryId, 'failed_at' => time()]);
    ftc_json(502, [
        'ok' => false,
        'error' => 'delivery_failed',
        'message' => 'We couldn\'t send your enquiry just now. Your details are still in the form – please try again in a moment.',
    ]);
}
