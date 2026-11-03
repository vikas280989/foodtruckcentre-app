<?php
declare(strict_types=1);

/**
 * POST /app-api/v1/quote.php
 * Event catering quote request – mirrors the website's Contact Form 7 form (id 102).
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

// Allowed values – identical to the website form.
const FTC_CUISINES = [
    'Burgers / American', 'Mexican / Latin', 'Asian Fusion', 'Vietnamese', 'Mediterranean / Middle Eastern',
    'BBQ / Smoked Meats', 'Pizza / Italian', 'Seafood', 'Vegan / Healthy', 'Desserts / Coffee / Sweets',
];
const FTC_EVENT_TYPES = ['Office Celebration', 'Birthday', 'Other'];
const FTC_PAYERS = ['Organiser', 'Guests'];
const FTC_YES_NO = ['Yes', 'No'];

$errors = [];

$name = ftc_clean($data['name'] ?? '', 100);
if (mb_strlen($name) < 2) {
    $errors['name'] = 'Please enter your full name.';
}
$email = ftc_clean($data['email'] ?? '', 254);
if (!ftc_valid_email($email)) {
    $errors['email'] = 'Please enter a valid email address.';
}
$phone = ftc_clean($data['phone'] ?? '', 20);
if (!ftc_valid_phone($phone)) {
    $errors['phone'] = 'Please enter a valid phone number.';
}

$eventDate = ftc_clean($data['event_date'] ?? '', 10);
$dt = DateTimeImmutable::createFromFormat('!Y-m-d', $eventDate, new DateTimeZone('Australia/Sydney'));
$today = new DateTimeImmutable('today', new DateTimeZone('Australia/Sydney'));
if (!$dt || $dt->format('Y-m-d') !== $eventDate) {
    $errors['event_date'] = 'Please choose the event date.';
} elseif ($dt < $today->modify('-1 day')) { // 1 day tolerance for time zones
    $errors['event_date'] = 'The event date can\'t be in the past.';
} elseif ($dt > $today->modify('+3 years')) {
    $errors['event_date'] = 'Please choose a date within the next 3 years.';
}

$foodTime = ftc_clean($data['food_time'] ?? '', 40);
$address = ftc_clean($data['event_address'] ?? '', 250);
$postcode = ftc_clean($data['postcode'] ?? '', 4);
if (!preg_match('/^\d{4}$/', $postcode)) {
    $errors['postcode'] = 'Please enter a valid 4-digit Australian postcode.';
}

$eventType = ftc_clean($data['event_type'] ?? '', 40);
if (!in_array($eventType, FTC_EVENT_TYPES, true)) {
    $errors['event_type'] = 'Please choose the type of event.';
}

$mealsRaw = $data['meals'] ?? null;
$meals = filter_var($mealsRaw, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 100000]]);
if ($meals === false || $meals < 50) {
    $errors['meals'] = 'The minimum number of meals is 50.';
}

$paying = ftc_clean($data['paying'] ?? '', 20);
if (!in_array($paying, FTC_PAYERS, true)) {
    $errors['paying'] = 'Please tell us who will be paying for food.';
}
$power = ftc_clean($data['power'] ?? '', 3);
if (!in_array($power, FTC_YES_NO, true)) {
    $errors['power'] = 'Please answer this question.';
}
$privateLand = ftc_clean($data['private_land'] ?? '', 3);
if (!in_array($privateLand, FTC_YES_NO, true)) {
    $errors['private_land'] = 'Please answer this question.';
}
$entrance = ftc_clean($data['entrance'] ?? '', 3);
if (!in_array($entrance, FTC_YES_NO, true)) {
    $errors['entrance'] = 'Please answer this question.';
}

$cuisines = $data['cuisines'] ?? [];
if (!is_array($cuisines) || !array_is_list($cuisines)) {
    $cuisines = [];
    $errors['cuisines'] = 'Please choose your cuisines again.';
}
$cuisines = array_values(array_unique(array_map(fn($c) => ftc_clean($c, 60), $cuisines)));
if (count($cuisines) > 3) {
    $errors['cuisines'] = 'Please select a maximum of three cuisines.';
}
foreach ($cuisines as $c) {
    if (!in_array($c, FTC_CUISINES, true)) {
        $errors['cuisines'] = 'Please choose your cuisines again.';
        break;
    }
}

$info = ftc_clean($data['additional_info'] ?? '', 3000, true);
if (ftc_link_count($info) > 3) {
    $errors['additional_info'] = 'Please remove some of the links from your message.';
}

if ($errors) {
    ftc_json(422, ['ok' => false, 'error' => 'validation', 'message' => 'Please check the highlighted fields.', 'fields' => $errors]);
}

$enquiryId = ftc_new_enquiry_id();
$displayDate = $dt->format('D j M Y');

$sections = [
    'Customer details' => [
        'Full name' => $name,
        'Email address' => $email,
        'Phone number' => $phone,
    ],
    'Cuisines (max. 3)' => [
        'Selected cuisines' => $cuisines ? implode(', ', $cuisines) : '',
    ],
    'Event details' => [
        'Event date' => $displayDate,
        'Time food is to be served' => $foodTime,
        'Event address' => $address,
        'Postcode' => $postcode,
        'Event type' => $eventType,
        'Number of meals' => (string)$meals,
    ],
    'Event requirements' => [
        'Who will be paying for food' => $paying,
        'Power available at the address' => $power,
        'Truck parked on private land' => $privateLand,
        'Entrance wider than 3m and at least 5m long, no overhanging obstacles' => $entrance,
        'Additional information' => $info,
    ],
];

// Subject: "New Food Truck Catering Enquiry – [Customer Name] – [Event Date]" (no line breaks possible: cleaned)
$subject = 'New Food Truck Catering Enquiry – ' . $name . ' – ' . $displayDate;

$message = [
    'subject' => $subject,
    'html' => ftc_email_html('New Food Truck Catering Enquiry', 'A customer has requested food truck catering quotes through the Food Truck Centre app.', $sections, $enquiryId),
    'text' => ftc_email_text('New Food Truck Catering Enquiry', $sections, $enquiryId),
    'reply_to_email' => $email,
    'reply_to_name' => $name,
];

$fingerprint = hash('sha256', 'quote|' . json_encode([$name, $email, $phone, $eventDate, $postcode, $eventType, $meals, $cuisines, $info]));
ftc_deliver_and_respond($idemKey, $fingerprint, $message, $enquiryId);
