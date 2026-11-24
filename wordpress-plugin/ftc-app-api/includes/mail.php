<?php
if (!defined('ABSPATH')) {
    exit;
}

/**
 * Build a branded HTML email from labelled rows.
 * $sections = ['Section title' => ['Label' => 'value', ...], ...]
 */
function ftc_app_email_html(string $heading, string $intro, array $sections, string $enquiryId): string
{
    $rows = '';
    foreach ($sections as $title => $fields) {
        $rows .= '<tr><td colspan="2" style="padding:22px 0 8px;font:700 13px/1.3 Arial,sans-serif;letter-spacing:.08em;text-transform:uppercase;color:#D62828;border-bottom:2px solid #F4B400;">' . ftc_app_h($title) . '</td></tr>';
        foreach ($fields as $label => $value) {
            $display = $value === '' ? '<span style="color:#94a3b8">Not provided</span>' : nl2br(ftc_app_h($value));
            $rows .= '<tr>'
                . '<td style="padding:10px 12px 10px 0;width:42%;vertical-align:top;font:600 14px/1.4 Arial,sans-serif;color:#0A1F5C;border-bottom:1px solid #eef0f4;">' . ftc_app_h($label) . '</td>'
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
        . '<div style="font:700 22px/1.3 Arial,sans-serif;color:#ffffff;margin-top:10px;">' . ftc_app_h($heading) . '</div>'
        . '</td></tr>'
        . '<tr><td style="height:5px;background:#D62828;"></td></tr>'
        . '<tr><td style="padding:22px 28px 6px;font:400 15px/1.6 Arial,sans-serif;color:#334155;">' . ftc_app_h($intro) . '</td></tr>'
        . '<tr><td style="padding:0 28px 24px;"><table role="presentation" width="100%" cellpadding="0" cellspacing="0">' . $rows . '</table></td></tr>'
        . '<tr><td style="padding:16px 28px;background:#f7f9fb;font:400 12px/1.6 Arial,sans-serif;color:#64748b;">'
        . 'Enquiry ID: <strong>' . ftc_app_h($enquiryId) . '</strong><br>Received: ' . ftc_app_h($received) . '<br>'
        . 'Sent from the Food Truck Centre Android app. Reply to this email to respond directly to the customer. All app enquiries are also listed in WordPress under App Enquiries.'
        . '</td></tr></table></td></tr></table></body></html>';
}

function ftc_app_email_text(string $heading, array $sections, string $enquiryId): string
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
 * Send one stored enquiry through wp_mail(), which uses the site's SMTP plugin when one is configured.
 * Returns [bool $sent, string $error].
 */
function ftc_app_send(array $row): array
{
    $to = (string) get_option('ftc_app_recipient', 'vikas.kumar617@gmail.com');
    if (!ftc_app_valid_email($to)) {
        return [false, 'Recipient email in Settings is not valid'];
    }
    $headers = ['Content-Type: text/html; charset=UTF-8'];
    $replyName = trim(str_replace(['"', '<', '>', ',', ';', "\r", "\n"], '', (string) $row['name']));
    if (ftc_app_valid_email((string) $row['email'])) {
        $headers[] = 'Reply-To: ' . ($replyName !== '' ? '"' . $replyName . '" ' : '') . '<' . $row['email'] . '>';
    }
    $error = '';
    $onFail = function ($wpError) use (&$error) {
        $error = is_wp_error($wpError) ? substr($wpError->get_error_message(), 0, 250) : 'wp_mail failed';
    };
    $altBody = function ($phpmailer) use ($row) {
        $phpmailer->AltBody = (string) $row['body_text'];
    };
    add_action('wp_mail_failed', $onFail);
    add_action('phpmailer_init', $altBody);
    try {
        $sent = (bool) wp_mail($to, (string) $row['subject'], (string) $row['body_html'], $headers);
    } catch (\Throwable $e) {
        $sent = false;
        $error = 'Mail exception: ' . get_class($e);
    }
    remove_action('wp_mail_failed', $onFail);
    remove_action('phpmailer_init', $altBody);
    if (!$sent && $error === '') {
        $error = 'wp_mail returned false';
    }
    return [$sent, $error];
}
