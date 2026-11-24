<?php
/**
 * Validation identical to the standalone app-api (and to the website's Contact Form 7 quote form options).
 * Each function returns ['errors' => [...]] or a prepared enquiry.
 */
if (!defined('ABSPATH')) {
    exit;
}

const FTC_APP_CUISINES = [
    'Burgers / American', 'Mexican / Latin', 'Asian Fusion', 'Vietnamese', 'Mediterranean / Middle Eastern',
    'BBQ / Smoked Meats', 'Pizza / Italian', 'Seafood', 'Vegan / Healthy', 'Desserts / Coffee / Sweets',
];
const FTC_APP_EVENT_TYPES = ['Office Celebration', 'Birthday', 'Other'];
const FTC_APP_PAYERS = ['Organiser', 'Guests'];
const FTC_APP_YES_NO = ['Yes', 'No'];
const FTC_APP_TOPICS = ['General enquiry', 'Corporate catering', 'Feedback', 'App support'];

function ftc_app_validate_quote(array $data): array
{
    $errors = [];

    $name = ftc_app_clean($data['name'] ?? '', 100);
    if (mb_strlen($name) < 2) {
        $errors['name'] = 'Please enter your full name.';
    }
    $email = ftc_app_clean($data['email'] ?? '', 254);
    if (!ftc_app_valid_email($email)) {
        $errors['email'] = 'Please enter a valid email address.';
    }
    $phone = ftc_app_clean($data['phone'] ?? '', 20);
    if (!ftc_app_valid_phone($phone)) {
        $errors['phone'] = 'Please enter a valid phone number.';
    }

    $eventDate = ftc_app_clean($data['event_date'] ?? '', 10);
    $tz = new DateTimeZone('Australia/Sydney');
    $dt = DateTimeImmutable::createFromFormat('!Y-m-d', $eventDate, $tz);
    $today = new DateTimeImmutable('today', $tz);
    if (!$dt || $dt->format('Y-m-d') !== $eventDate) {
        $errors['event_date'] = 'Please choose the event date.';
    } elseif ($dt < $today->modify('-1 day')) {
        $errors['event_date'] = 'The event date can\'t be in the past.';
    } elseif ($dt > $today->modify('+3 years')) {
        $errors['event_date'] = 'Please choose a date within the next 3 years.';
    }

    $foodTime = ftc_app_clean($data['food_time'] ?? '', 40);
    $address = ftc_app_clean($data['event_address'] ?? '', 250);
    $postcode = ftc_app_clean($data['postcode'] ?? '', 4);
    if (!preg_match('/^\d{4}$/', $postcode)) {
        $errors['postcode'] = 'Please enter a valid 4-digit Australian postcode.';
    }
    $eventType = ftc_app_clean($data['event_type'] ?? '', 40);
    if (!in_array($eventType, FTC_APP_EVENT_TYPES, true)) {
        $errors['event_type'] = 'Please choose the type of event.';
    }
    $meals = filter_var($data['meals'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 100000]]);
    if ($meals === false || $meals < 50) {
        $errors['meals'] = 'The minimum number of meals is 50.';
    }
    $paying = ftc_app_clean($data['paying'] ?? '', 20);
    if (!in_array($paying, FTC_APP_PAYERS, true)) {
        $errors['paying'] = 'Please tell us who will be paying for food.';
    }
    $answers = [];
    foreach (['power', 'private_land', 'entrance'] as $k) {
        $answers[$k] = ftc_app_clean($data[$k] ?? '', 3);
        if (!in_array($answers[$k], FTC_APP_YES_NO, true)) {
            $errors[$k] = 'Please answer this question.';
        }
    }

    $cuisines = $data['cuisines'] ?? [];
    if (!is_array($cuisines) || !array_is_list($cuisines)) {
        $cuisines = [];
        $errors['cuisines'] = 'Please choose your cuisines again.';
    }
    $cuisines = array_values(array_unique(array_map(fn($c) => ftc_app_clean($c, 60), $cuisines)));
    if (count($cuisines) > 3) {
        $errors['cuisines'] = 'Please select a maximum of three cuisines.';
    }
    foreach ($cuisines as $c) {
        if (!in_array($c, FTC_APP_CUISINES, true)) {
            $errors['cuisines'] = 'Please choose your cuisines again.';
            break;
        }
    }

    $info = ftc_app_clean($data['additional_info'] ?? '', 3000, true);
    if (ftc_app_link_count($info) > 3) {
        $errors['additional_info'] = 'Please remove some of the links from your message.';
    }

    if ($errors) {
        return ['errors' => $errors];
    }

    $displayDate = $dt->format('D j M Y');
    return [
        'type' => 'quote',
        'name' => $name,
        'email' => $email,
        'phone' => $phone,
        'event_date' => $eventDate,
        'subject' => 'New Food Truck Catering Enquiry – ' . $name . ' – ' . $displayDate,
        'heading' => 'New Food Truck Catering Enquiry',
        'intro' => 'A customer has requested food truck catering quotes through the Food Truck Centre app.',
        'sections' => [
            'Customer details' => ['Full name' => $name, 'Email address' => $email, 'Phone number' => $phone],
            'Cuisines (max. 3)' => ['Selected cuisines' => $cuisines ? implode(', ', $cuisines) : ''],
            'Event details' => [
                'Event date' => $displayDate,
                'Time food is to be served' => $foodTime,
                'Event address' => $address,
                'Postcode' => $postcode,
                'Event type' => $eventType,
                'Number of meals' => (string) $meals,
            ],
            'Event requirements' => [
                'Who will be paying for food' => $paying,
                'Power available at the address' => $answers['power'],
                'Truck parked on private land' => $answers['private_land'],
                'Entrance wider than 3m and at least 5m long, no overhanging obstacles' => $answers['entrance'],
                'Additional information' => $info,
            ],
        ],
        'fingerprint' => hash('sha256', 'quote|' . wp_json_encode([$name, $email, $phone, $eventDate, $postcode, $eventType, $meals, $cuisines, $info])),
    ];
}

function ftc_app_validate_contact(array $data): array
{
    $errors = [];
    $name = ftc_app_clean($data['name'] ?? '', 100);
    if (mb_strlen($name) < 2) {
        $errors['name'] = 'Please enter your full name.';
    }
    $email = ftc_app_clean($data['email'] ?? '', 254);
    if (!ftc_app_valid_email($email)) {
        $errors['email'] = 'Please enter a valid email address.';
    }
    $topic = ftc_app_clean($data['topic'] ?? 'General enquiry', 40);
    if (!in_array($topic, FTC_APP_TOPICS, true)) {
        $errors['topic'] = 'Please choose a topic.';
    }
    $message = ftc_app_clean($data['message'] ?? '', 3000, true);
    if (mb_strlen($message) < 5) {
        $errors['message'] = 'Please write a short message.';
    } elseif (ftc_app_link_count($message) > 3) {
        $errors['message'] = 'Please remove some of the links from your message.';
    }
    if ($errors) {
        return ['errors' => $errors];
    }
    $sections = [
        'Message' => ['Topic' => $topic, 'Message' => $message],
        'From' => ['Full name' => $name, 'Email address' => $email],
    ];
    if ($topic === 'App support') {
        $sections['App'] = [
            'App version' => ftc_app_clean($data['app_version'] ?? '', 30),
            'Device' => ftc_app_clean($data['device'] ?? '', 80),
        ];
    }
    return [
        'type' => 'contact',
        'name' => $name,
        'email' => $email,
        'phone' => '',
        'event_date' => '',
        'subject' => 'New ' . $topic . ' – ' . $name . ' (Food Truck Centre app)',
        'heading' => 'New ' . $topic,
        'intro' => 'A message was sent from the Contact screen of the Food Truck Centre app.',
        'sections' => $sections,
        'fingerprint' => hash('sha256', 'contact|' . wp_json_encode([$name, $email, $topic, $message])),
    ];
}
