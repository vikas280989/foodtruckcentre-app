=== Food Truck Centre App API ===
Requires at least: 6.0
Requires PHP: 8.1
Stable tag: 1.0.0
License: GPLv2 or later

Receives enquiries from the Food Truck Centre Android app.

== Description ==
* Answers the app at /app-api/v1/quote.php, /app-api/v1/contact.php and /app-api/v1/app-config.php
  (the addresses built into app v1.0.0) and at /wp-json/ftc/v1/quote|contact|app-config.
* Emails every enquiry with wp_mail(), so it uses the site's existing SMTP plugin – no new password.
* Saves every enquiry under "App Enquiries" in the WordPress admin, with delivery status.
* Retries undelivered enquiries every 15 minutes (WP-Cron), up to 20 attempts.
* Spam protection: HTTPS only, JSON only, 16 KB limit, per-IP rate limit, honeypot, minimum fill time,
  strict validation matching the website form, duplicate protection.
* No payments. No changes to pages, themes or other plugins.

== Settings ==
App Enquiries → Settings: enquiry recipient (default vikas.kumar617@gmail.com), email shown in the app,
spam limits, "Send a test email" and "Retry undelivered enquiries now".

== Uninstall ==
Deactivating stops the scheduled retry. Deleting the plugin leaves the enquiries table
({prefix}ftc_app_enquiries) in place so no customer enquiries are lost; drop it manually if no longer needed.
