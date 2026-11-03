# Food Truck Centre – Android app

Native Android app for **Food Truck Centre Australia** ([foodtruckcentre.com.au](https://foodtruckcentre.com.au/)).
Customers can browse the catering services, see the featured food truck, request quotes and contact the business.
**The app has no payment functionality.**

| | |
|---|---|
| App name | Food Truck Centre |
| Package | `au.com.foodtruckcentre.app` (debug build: `au.com.foodtruckcentre.app.debug`) |
| Platform | Android 8.0 (API 26) and newer, targets API 36 |
| Stack | Kotlin, Jetpack Compose, Material 3, OkHttp, kotlinx.serialization, Coil |
| Backend | Small PHP API (`backend/app-api`) for your existing Hostinger hosting, sending email via SMTP (PHPMailer) |
| Permissions | `INTERNET` only |

## What's in this repository

```
android/            Android Studio project (Kotlin + Compose)
backend/app-api/    PHP email API – upload to public_html/app-api/
backend/tests/      Automated API tests (45 checks)
ci/                 Emulator test + dependency vulnerability scripts used by GitHub Actions
.github/workflows/  Build pipeline: tests → lint → signed build → emulator test → GitHub Release
branding/           Logo and icon source files
docs/               Deployment, signing, security audit, test report, limitations
```

## Screens

1. **Splash** – branded system splash, then an animated logo intro
2. **Home** – hero “Book Australia’s Best Food Trucks”, the website’s three highlights, How to Book, cuisines, services, featured truck, contact, footer
3. **Request a Quote** – the website’s event enquiry form: 10 cuisines (max 3), name, email, phone, date, serving time, address, postcode, event type, meals (min 50), the four event requirement questions and additional information
4. **Catering Services** – the six services published on the website, each with *Request a Quote*
5. **Featuring** – loaded live from the website’s Featuring page (no hard-coded truck)
6. **Contact Us** – contact form plus email and Instagram
7. **Privacy Policy / Terms and Conditions / Vendor Terms** – loaded live from the website; an offline copy is bundled
8. **How to Book** – the process explained from the website’s own content
9. **Success** – confirmation shown only after the server confirms the email was accepted

## How content stays up to date without a new APK

- **Featuring** and the **three legal pages** are read from the website’s public WordPress REST API (`/wp-json/wp/v2/pages?slug=…`). Edit them in WordPress and the app shows the change.
- The **enquiry recipient** and the **email address shown on the Contact screen** live in the server config (`config.php`), not in the app.
- Home-screen text, cuisines and services are bundled; changing them needs a new build. See `android/app/src/main/java/au/com/foodtruckcentre/app/data/SiteContent.kt`.

## Building

GitHub Actions builds everything automatically on every push to `main` (see `docs/SIGNING.md` for the four required secrets). Download the APKs from the repository’s **Releases** page.

To build locally in Android Studio: open the `android` folder, create `android/keystore.properties` (see `docs/SIGNING.md`) and run **Build → Generate App Bundles or APKs → Build APK(s)**, or `./gradlew assembleRelease`.

## Documentation

- [Backend deployment and email configuration](docs/DEPLOYMENT.md)
- [Release signing](docs/SIGNING.md)
- [Security and privacy audit](docs/SECURITY-AUDIT.md)
- [Functional test report](docs/TEST-REPORT.md)
- [Known limitations and required configuration](docs/LIMITATIONS.md)
