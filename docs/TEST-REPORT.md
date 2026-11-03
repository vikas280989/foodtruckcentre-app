# Functional test report – Food Truck Centre app v1.0.0

Final verified build: **GitHub Actions run 41647244405** (commit `c85b74e`), 3 November 2026.
Result: **all jobs passed**. Release: `v1.0.0-build.5`.

Only tests that were actually executed are listed. Anything not tested is listed at the end.

## 1. Backend API – 45/45 passed

Run on every build (`backend/tests/run_tests.py`) using PHP's built-in server and a test mail transport.

| Area | Checks |
|---|---|
| Delivery | Valid quote accepted. Email goes to the configured recipient. Subject is `New Food Truck Catering Enquiry – [Name] – [Date]`. All fields are present. Reply-To is the customer. Contact and App support messages are delivered. |
| Validation | Rejects: empty name, invalid email, email header injection, invalid phone, meals < 50, meals not a number, > 3 cuisines, unknown cuisine, past date, impossible date, bad postcode, event type not on the website, missing *paying / power / private land / entrance*, too many links, missing contact message, unknown topic |
| Security | XSS is escaped in the email. SQL-like input is treated as text. The honeypot blocks bots. Too-fast submissions are blocked. Invalid submission IDs are rejected. GET, non-JSON, JSON arrays and oversized bodies are rejected. Plain-HTTP requests are refused. Config doesn't leak. Rate limit returns 429 after the limit. |
| Duplicates | The same submission ID isn't emailed twice. Identical content with a new ID isn't emailed twice. |
| Failure handling | When mail fails, the app gets an error (**no false success**). The enquiry is preserved on the server. A retry succeeds and clears the preserved copy. The real SMTP code path fails cleanly against an unreachable server. |

## 2. Android unit tests – passed

`testDebugUnitTest` and `testReleaseUnitTest`: email, phone, meals ≥ 50, event date (no past dates, ≤ 3 years ahead), 4-digit postcode, max 3 cuisines, cuisine detection on the Featuring page, website HTML parsing (numbered legal headings, bullets, links, images, entities, script stripping, empty pages).

## 3. Android lint (release) – passed

0 errors. One error was found and fixed in commit `a60c1e6` (`ContextCastToActivity` → `LocalActivity`). Lint isn't suppressed and no baseline is used.

## 4. Build, signing and APK checks – passed

| Check | Result |
|---|---|
| Release APK signed | Yes, v2 + v3 signature schemes (v1 isn't needed because minSdk is 26) |
| Signer | `CN=Food Truck Centre, O=Food Truck Centre, L=Sydney, ST=NSW, C=AU` |
| Certificate SHA-256 | `6bdc8fdf bd4ae100 accf8940 718b8c85 5e79c766 ae7818f1 b0034b25 600e0945` (matches the key in your `signing` folder) |
| Package / version | `au.com.foodtruckcentre.app`, versionCode 1, versionName 1.0.0, targetSdk 36, minSdk 26 |
| Permissions | `android.permission.INTERNET` only. The other entry, `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, is an internal permission that AndroidX adds automatically. It's private to the app, invisible to users and grants nothing. |
| Secrets in APK | None (CI scan, plus a separate manual scan of the downloaded APK) |
| Payment code | None. No Stripe, PayPal, Razorpay, Braintree or Play Billing classes. |
| Network endpoints in code | `https://foodtruckcentre.com.au/app-api/v1/…` and `https://foodtruckcentre.com.au/wp-json/…` only, HTTPS |
| Dependency vulnerabilities | 117 release dependencies checked against osv.dev: **no known vulnerabilities** |
| Sizes | Release 3.0 MB, debug 19.9 MB |
| SHA-256 | Release `7a591f37…b8fe`, debug `f49a0b51…291d` (full values in `SHA256SUMS.txt`) |

## 5. Device test (Android 14 emulator, Pixel 6 profile) – 20/20 passed

| # | Test | Result |
|---|---|---|
| 1 | Install release APK | PASS |
| 2 | Install debug APK (installs alongside) | PASS |
| 3–4 | Both packages present | PASS |
| 5 | Only the INTERNET permission is requested | PASS |
| 6 | App launches and keeps running | PASS |
| 7 | Home hero "Book Australia’s Best Food Trucks" | PASS |
| 8 | Home sections: How to Book, cuisines, catering services, featured truck (live from the website: *thepizzaboys*), contact | PASS |
| 9–12 | Bottom navigation: Services, Featuring, Contact, Request a Quote | PASS |
| 13 | Cuisine list shows the website's 10 categories | PASS |
| 14 | 4th cuisine blocked with the "maximum of three cuisines" message | PASS |
| 15 | Counter shows 3/3 | PASS |
| 16 | Empty form: submission blocked, scrolls to the first field, shows its message | PASS |
| 17 | Privacy Policy opens (live from the website) | PASS |
| 18 | App resumes after going to the background | PASS |
| 19 | Debug build launches | PASS |
| 20 | No app crashes during the whole session | PASS |

Screenshots of each screen are attached to the run as the `device-test-results` artifact.

Earlier runs showed failures that turned out to be test-script problems: a section check after scrolling, and the emulator's "System UI isn't responding" dialog. The script was fixed. No app crash was ever recorded.

## Not tested yet (needs your action)

- **Real email delivery** from the app to `vikas.kumar617@gmail.com`. This needs the backend uploaded to your hosting with your SMTP password ([DEPLOYMENT.md](DEPLOYMENT.md)). Until then, the app's forms correctly report that sending failed.
- **A successful submission and the Success screen on a device** (depends on the above).
- **Physical phones and small screens.** Please install the APK on your own Android phone.
- The date and time pickers were exercised by code review and unit tests of the date rules, not by automated taps.
