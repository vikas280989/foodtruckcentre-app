# Security and privacy review – Food Truck Centre app v1.0.0

Reviewed 3 November 2026 against the project's 20 security requirements, the published
Food Truck Centre Privacy Policy and the Australian Privacy Principles (APPs).
"Verified by" says how each item was checked. *Automated* means it is enforced on every build.

## Summary

No secrets are in the app, all traffic is HTTPS, the app requests only the `INTERNET` permission,
and every enquiry is validated server-side and delivered only to the configured recipient.
There is no payment code, SDK or screen.

## Requirement checklist

| # | Requirement | Implementation | Verified by |
|---|---|---|---|
| 1 | HTTPS only | `network_security_config.xml` sets `cleartextTrafficPermitted="false"` and trusts system CAs only. `usesCleartextTraffic=false`. All URLs are `https://`. The API also refuses non-HTTPS requests, and `.htaccess` blocks HTTP. | Code review; backend test "plain HTTP requests refused" |
| 2 | No hard-coded API secrets | The app holds only public URLs. No API keys exist. | CI step "Check for secrets … inside the APK" |
| 3 | No SMTP credentials in app | SMTP is configured only in the server's `config.php`, outside the web root. | CI APK scan; code review |
| 4 | Server-side input validation | Every field is whitelisted, length-limited and type-checked. Dropdown and cuisine values must exactly match the website's options. | 17 backend rejection tests |
| 5 | Server-side email validation | `FILTER_VALIDATE_EMAIL`, 254-char limit, and newline/`,;<>` rejection (header-injection) | Tests "invalid email", "header injection in email" |
| 6 | Request size limits | 16 KB in PHP, plus `LimitRequestBody` in `.htaccess`. Text fields are capped (message 3,000 chars). | Test "oversized body rejected" |
| 7 | Rate limiting | Per-IP sliding window: 5 per 10 min, 20 per day (configurable). Returns HTTP 429. | Test "rate limit kicks in after 3" (low limit) |
| 8 | Spam/bot protection | Hidden honeypot field, minimum fill time, max 3 links per message, JSON-only API | Tests "honeypot", "too-fast submission", "too many links" |
| 9 | Duplicate submission protection | The app disables the button while sending and uses one UUID per form. The server returns the original result for a repeated UUID or identical content within 30 minutes, without a second email. | Tests "same submission_id…", "identical content…" |
| 10 | Secure error handling | Customers see friendly messages only. PHP `display_errors=0`. Errors are logged without personal data or credentials. | Code review; SMTP failure test |
| 11 | No sensitive data in logs | The app has no logging of form data, and release builds strip `Log.v/d/i`. The server logs only exception class names. | Code review |
| 12 | No exposed DB credentials | There is no database. | n/a |
| 13 | No unnecessary permissions | Only `android.permission.INTERNET` | CI checks the APK manifest and fails on any other permission. The emulator test checks requested permissions. |
| 14 | No insecure HTTP | See #1 | as #1 |
| 15 | Dependency vulnerability checks | Every resolved release dependency is checked against osv.dev on each build (`ci/osv_check.py`). Dependencies are pinned in `gradle/libs.versions.toml`. | CI artifact `vulnerability-scan.txt` |
| 16 | SQL injection | No SQL is used anywhere. Input is treated as text. | Test "SQL-like input handled as plain text" |
| 17 | XSS | Email output uses `htmlspecialchars` (ENT_QUOTES, UTF-8). Website HTML is parsed into plain text blocks in the app. No WebView, so no script can run. | Test "HTML escapes user input" plus parser unit test |
| 18 | Malicious submissions | JSON-only, method and content-type checks, UTF-8 validation, control characters stripped, nesting depth limit (8) | Tests "non-JSON rejected", "JSON array rejected", "GET not allowed" |
| 19 | Secure backend config | Config lives outside the web root (fallback is `.htaccess` deny). Storage files are `0600`. Security headers: nosniff, DENY, no-referrer, CSP `default-src 'none'`, HSTS, `no-store`. | Test "config does not leak" |
| 20 | Personal information handling | See below | Review |

## Privacy (APPs and the published Privacy Policy)

- **Collection is limited to what the website form already collects.** For App support messages only, the app adds device model and app version, and only for that topic.
- **No storage on the device.** Form contents live in memory only, are cleared after a successful send, and are excluded from Android backup (`allowBackup=false` plus data-extraction rules).
- **No analytics, advertising, crash-reporting or tracking SDKs.** No location, contacts, camera or microphone access.
- **Server retention:** the server keeps only hashed IP counters (24 h), hashed submission IDs (7 days) and content fingerprints (24 h). Undelivered enquiries are kept in a private folder until re-sent, then deleted. Delivered enquiries live only in the recipient mailbox.
- **Notice:** every form links to the live Privacy Policy and Terms. The quote screen states that an enquiry is not a confirmed booking and that no payment is taken.
- **Disclosure:** data goes only to the configured recipient by email (Reply-To is the customer).

## Android hardening

- R8 minification and resource shrinking on release
- No exported components except the launcher activity
- No WebView, no JavaScript bridge, no file provider
- Links open only for `https://` (and `mailto:`)
- Release signed with v1 + v2 + v3 schemes (RSA 4096). The CI verifies the signer is "CN=Food Truck Centre".
- Keystore never committed. In CI it exists only for the duration of the build and comes from encrypted GitHub Secrets.

## Residual risks and recommendations

1. **Bot protection without CAPTCHA.** A determined attacker can script the public API within the rate limits. If spam appears, add Cloudflare in front of the site or enable Cloudflare Turnstile (needs a site key; no change to the email flow).
2. **Shared hosting.** The API's security also depends on the Hostinger account. Use 2FA on hPanel and a unique SMTP password.
3. **Website content** (Featuring, legal pages) is displayed as text from the website. Anyone with WordPress admin access controls that content. Protect WordPress admin accounts.
4. **Legal text** is shown exactly as published. Note that the Terms list `info@hellofoodtrucks.com.au` as the contact address, which differs from the other pages. This was not changed (legal text was not rewritten). Please review it on the website.
5. Rate limits are per IP, so customers behind the same mobile carrier NAT share a limit. The defaults are generous, and you can adjust them in `config.php`.
