<?php
return [
    'recipient_email' => 'info@foodtruckcentre.com.au',
    'recipient_name' => 'Food Truck Centre',
    'public_contact_email' => 'info@foodtruckcentre.com.au',
    'mail' => ['driver' => 'log', 'from_email' => 'app@example.test', 'from_name' => 'Test'],
    'rate_limit' => ['per_10_minutes' => (int)(getenv('FTC_TEST_RL') ?: 1000), 'per_day' => 5000],
    'min_fill_seconds' => 4,
    'max_body_bytes' => 16384,
    'storage_dir' => getenv('FTC_TEST_STORAGE'),
];
