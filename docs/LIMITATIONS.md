# Known limitations and required external configuration

## You must do this before the forms work

1. **Upload and configure the backend.** Follow [DEPLOYMENT.md](DEPLOYMENT.md): upload `app-api`, create `config.php` and enter your SMTP mailbox password.
   Until then, the Quote and Contact forms honestly report that sending failed.
2. **Keep the signing key safe** ([SIGNING.md](SIGNING.md)).

## Design decisions you should know about

- **Website form parity.** The quote form uses exactly the website's fields and options. Event type is *Office Celebration / Birthday / Other*, and the payer is *Organiser / Guests*.
  Only *Corporate Catering* preselects an event type (*Office Celebration*), because the other services don't map clearly to the website's three options.
- **Cuisines are optional, max 3**, as on the website.
- **Extra validation beyond the website:** postcode must be 4 digits (Australia), the event date can't be in the past or more than 3 years ahead, phone must have 8–15 digits, and the contact message is required.
- **The contact form adds a topic selector** (General enquiry / Corporate catering / Feedback / App support) so you can tell enquiries apart. Every topic goes to the same recipient.
- **All enquiries go to the address in `config.php`**, not to the website's cuisine-to-vendor routing in Contact Form 7. If you want app enquiries forwarded to vendors by cuisine, that can be added on the server later.
- **The app doesn't email the customer a copy.** The customer sees an in-app confirmation with an enquiry reference. An automatic customer email was left out deliberately, because a public form that sends email to any address can be abused to send spam.
- **Bundled content.** The home text, the three highlights ("500+ Events booked", "6+ Years of experience"), the cuisines and the six services are bundled from the website as of 3 Nov 2026. If they change on the website, update `SiteContent.kt` and rebuild.
- **Live content.** Featuring and the legal pages load from the website's public WordPress REST API. If that API is disabled by a security plugin, the legal pages fall back to the bundled copy and Featuring shows a "couldn't load" state.
- **Imagery.** All photos and service illustrations are the website's own images. No stock imagery is presented as real vendors or events.
- **Portrait only**, light theme only, English only.
- **minSdk 26 (Android 8.0).** This covers the vast majority of phones in use. Older devices can't install the app.

## Not included (not offered by the website)

There is no payment of any kind, no accounts or log-in, no vendor/customer dashboards, no booking management, no chat, no reviews or ratings, and no push notifications.

## Testing limits

- Automated device testing runs on an Android 14 emulator (Pixel 6 profile) in GitHub Actions. It hasn't been tested on a physical phone yet; please install the APK on your own phone too.
- End-to-end delivery to the real mailbox can only be verified after the backend is deployed with your SMTP credentials. The backend was tested with a test mail transport, and its SMTP path was tested against an unreachable server, which correctly returned a failure.
