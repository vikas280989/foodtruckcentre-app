<?php
declare(strict_types=1);

/**
 * POST /app-api/v1/contact.php
 * General contact / feedback / app support messages.
 */

require __DIR__ . '/../lib/bootstrap.php';
require __DIR__ . '/../lib/mailer.php';

ftc_config();
$data = ftc_require_post_json();
ftc_rate_limit('submit');

if (ftc_looks_like_bot($data)) {
    ftc_json(400, ['ok' => false, 'error' => 'rejected', 'message' => 'Please take a moment to check your details and submit again.']);
}
$idemKey = ftc_idempotency_key($data);

const FTC_TOPICS = ['General enquiry', 'Corporate catering', 'Feedback', 'App support'];

$errors = [];
$name = ftc_clean($data['name'] ?? '', 100);
if (mb_strlen($name) < 2) {
    $errors['name'] = 'Please enter your full name.';
}
$email = ftc_clean($data['email'] ?? '', 254);
if (!ftc_valid_email($email)) {
    $errors['email'] = 'Please enter a valid email address.';
}
$topic = ftc_clean($data['topic'] ?? 'General enquiry', 40);
if (!in_array($topic, FTC_TOPICS, true)) {
    $errors['topic'] = 'Please choose a topic.';
}
$messageText = ftc_clean($data['message'] ?? '', 3000, true);
if (mb_strlen($messageText) < 5) {
    $errors['message'] = 'Please write a short message.';
} elseif (ftc_link_count($messageText) > 3) {
    $errors['message'] = 'Please remove some of the links from your message.';
}
$appVersion = ftc_clean($data['app_version'] ?? '', 30);
$device = ftc_clean($data['device'] ?? '', 80);

if ($errors) {
    ftc_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Please check the highlighted fields.', 'fields' => $errors]);
}

$enquiryId = ftc_new_enquiry_id();
$sections = [
    'Message' => [
        'Topic' => $topic,
        'Message' => $messageText,
    ],
    'From' => [
        'Full name' => $name,
        'Email address' => $email,
    ],
];
if ($topic === 'App support') {
    $sections['App'] = ['App version' => $appVersion, 'Device' => $device];
}

$message = [
    'subject' => 'New ' . $topic . ' – ' . $name . ' (Food Truck Centre app)',
    'html' => ftc_email_html('New ' . $topic, 'A message was sent from the Contact screen of the Food Truck Centre app.', $sections, $enquiryId),
    'text' => ftc_email_text('New ' . $topic, $sections, $enquiryId),
    'reply_to_email' => $email,
    'reply_to_name' => $name,
];

$fingerprint = hash('sha256', 'contact|' . json_encode([$name, $email, $topic, $messageText]));
ftc_deliver_and_respond($idemKey, $fingerprint, $message, $enquiryId);
