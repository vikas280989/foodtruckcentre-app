<?php
/**
 * Food Truck Centre app API – server configuration.
 *
 * 1. Copy this file to "config.php" in the SAME folder.
 * 2. Fill in the SMTP details (the same mailbox you use for the website's SMTP plugin is fine).
 * 3. Never upload config.php to GitHub or share it.
 *
 * For best security, move the whole "private" folder OUTSIDE public_html
 * (e.g. /home/<you>/ftc-app-private/) and set the path in app-api/lib/bootstrap.php
 * – see DEPLOYMENT.md. When it stays inside public_html, the bundled .htaccess blocks web access to it.
 */
return [
    // Every enquiry from the app is delivered here. This is the ONLY place the recipient is set.
    'recipient_email' => 'vikas.kumar617@gmail.com',
    'recipient_name'  => 'Food Truck Centre',

    // Email address shown on the app's Contact screen ("Email us" button). Leave empty to hide the button.
    'public_contact_email' => 'vikas.kumar617@gmail.com',

    // Outgoing mail. driver: "smtp" (recommended), "mail" (PHP mail(), not recommended), "log" (testing only – nothing is sent).
    'mail' => [
        'driver'     => 'smtp',
        'host'       => 'smtp.hostinger.com',
        'port'       => 465,
        'encryption' => 'ssl',            // "ssl" for port 465, "tls" for port 587
        'username'   => 'app@foodtruckcentre.com.au',
        'password'   => 'CHANGE_ME',
        'from_email' => 'app@foodtruckcentre.com.au', // must be a mailbox your SMTP account is allowed to send as
        'from_name'  => 'Food Truck Centre App',
        'timeout'    => 20,
    ],

    // Abuse protection
    'rate_limit' => [
        'per_10_minutes' => 5,   // submissions per device/IP per 10 minutes
        'per_day'        => 20,  // submissions per IP per 24 hours
    ],
    'min_fill_seconds' => 4,      // forms completed faster than this are treated as bots
    'max_body_bytes'   => 16384,  // reject larger requests

    // Folder for rate-limit counters, duplicate protection and undelivered enquiries.
    // Default: the "storage" folder next to this file.
    'storage_dir' => __DIR__ . '/storage',

    // Set true only while testing (shows extra error detail in the server log, never to the app).
    'debug' => false,
];
