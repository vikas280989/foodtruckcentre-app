# Backend deployment and email configuration

The app sends every enquiry to a small PHP API on your existing hosting. This sits in its own
folder and **does not change WordPress, its plugins or its database**.

App endpoints (already built into the app):

| Endpoint | Purpose |
|---|---|
| `https://foodtruckcentre.com.au/app-api/v1/quote.php` | Quote request (POST, JSON) |
| `https://foodtruckcentre.com.au/app-api/v1/contact.php` | Contact / feedback / app support (POST, JSON) |
| `https://foodtruckcentre.com.au/app-api/v1/app-config.php` | Public contact email shown in the app (GET) |

> Until these are uploaded, the app's forms will show *"We couldn't send your enquiry right now"*. That's deliberate: the app never shows a false success.

## 1. Create a mailbox for sending (recommended)

In Hostinger hPanel → **Emails**, create a mailbox such as `app@foodtruckcentre.com.au`.
You can reuse the mailbox your WordPress SMTP plugin already uses instead.

## 2. Upload the API

1. hPanel → **File Manager** → open `public_html`.
2. Upload the folder `backend/app-api` so you end up with `public_html/app-api/v1/quote.php`, etc.
3. **Recommended:** move `public_html/app-api/private` to **`/home/<your-user>/ftc-app-private`**, one level *above* `public_html`. The API looks there first. If you leave it inside `public_html`, the included `.htaccess` blocks web access to it.

## 3. Configure

1. In the private folder, copy `config.sample.php` to **`config.php`**.
2. Edit `config.php`:
   - `recipient_email` – where every enquiry goes (currently `vikas.kumar617@gmail.com`). This is the **only** place it is set.
   - `public_contact_email` – shown on the app's Contact screen (leave `''` to hide it).
   - `mail` → `host`, `port`, `encryption`, `username`, `password`, `from_email`.
     Hostinger: `smtp.hostinger.com`, port `465`, encryption `ssl`.
3. Make sure the `storage` folder is writable by PHP (permissions 700 or 755).

## 4. Test

- Open `https://foodtruckcentre.com.au/app-api/v1/app-config.php` in a browser. You should see `{"ok":true,...,"contact_email":"…"}`.
- Opening `https://foodtruckcentre.com.au/app-api/private/config.php` must give **403 Forbidden**.
- Send a test quote from the app. Within a minute you should receive an email titled
  *New Food Truck Catering Enquiry – [Name] – [Date]*. **Reply** goes straight to the customer.

## 5. Retry undelivered enquiries (recommended)

If the mail server is down when someone submits, the app tells them it failed and keeps their form filled in so they can retry. The server also keeps a private copy in `storage/failed/`.
Add a Hostinger **Cron Job** (hPanel → Advanced → Cron Jobs) every 15 minutes:

```
php /home/<your-user>/public_html/app-api/tools/retry-failed.php
```

## Protections built in

HTTPS-only, JSON-only, 16 KB request limit, per-IP rate limit (5 per 10 min / 20 per day), honeypot field,
minimum form-fill time, strict whitelist validation that matches the website form, duplicate protection
(same submission ID or identical content within 30 minutes is not emailed twice), HTML escaping in
emails, header-injection protection, no database, and no credentials in the app.

## Changing the recipient later

Edit `recipient_email` in `config.php`. No app update needed.
