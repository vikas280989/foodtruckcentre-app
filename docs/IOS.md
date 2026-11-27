# Food Truck Centre – iOS app

Native SwiftUI app (iOS 16+, iPhone). Same features, wording, brand and backend as Android 1.1.0:
login required (email + password), quote form, contact form, Services, live Featuring page,
live legal pages (bundled offline copy), About, My account with enquiry progress, profile edit,
password reset, account deletion. No payments of any kind.

| | |
|---|---|
| Bundle ID | `au.com.foodtruckcentre.app` |
| Version | 1.1.0 (build 2) |
| Backend | `https://foodtruckcentre.com.au/app-api/v1/` (WordPress plugin "FTC App API" ≥ 1.1.1) |
| Session | Token in the iOS Keychain (`AfterFirstUnlockThisDeviceOnly`, never synced), sent as `X-FTC-Token` |
| Network | HTTPS only. Debug builds alone can be pointed at a local test server by the UI tests |
| Project | `ios/project.yml` (XcodeGen) – `brew install xcodegen && cd ios && xcodegen generate` |

## Code layout

```
ios/FoodTruckCentre/App          app entry, navigation, tab bar
ios/FoodTruckCentre/Data         API client + Keychain session, models, validation, website HTML parser, site content
ios/FoodTruckCentre/UI           theme (brand colours, Poppins), components, screens
ios/FoodTruckCentre/Resources    Poppins fonts, bundled legal pages, images, app icon
ios/FoodTruckCentreTests         unit tests (validation, parser, payload contract)
ios/FoodTruckCentreUITests       end-to-end test: register → quote → My enquiries → logout → login
```

## Automated testing (GitHub Actions, `.github/workflows/ios.yml`)

On every push that touches `ios/`, the plugin or CI, a macOS runner:

1. starts a throw-away local WordPress with the FTC plugin on 127.0.0.1:8800 (never the live site);
2. runs the unit tests and the end-to-end UI test on the iPhone simulator;
3. checks on the server that the enquiry was stored, emailed (to a captured outbox) and linked to the new account;
4. builds the Release app for devices and checks it only contains the live HTTPS API, no test override and no secrets;
5. saves the simulator screenshots as the `ios-test-results` artifact.

## Publishing (client's Apple account)

Apple requires the app to be published from an Apple Developer Program account (US$99/year).
Publishing under the client's own name means the App Store shows "Food Truck Centre" as the seller.

1. **Client:** get a free D-U-N-S number for the business (Apple's D-U-N-S lookup page; can take a few days).
2. **Client:** enrol at developer.apple.com/programs/enroll as an **Organisation**, using the business email/domain.
3. **Client:** in App Store Connect › Users and Access, invite the developer with the **Admin** role.
4. **Developer:** App Store Connect › Apps › “+” › New App – iOS, name “Food Truck Centre”,
   bundle ID `au.com.foodtruckcentre.app` (registered automatically by the first upload, or under Identifiers).
5. **Developer:** Users and Access › Integrations › App Store Connect API › generate a key with the **App Manager** role.
   Download the `.p8` once.
6. Add GitHub Secrets (repo › Settings › Secrets and variables › Actions):
   `APPLE_TEAM_ID`, `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8_BASE64` (`base64 -i AuthKey_XXXX.p8`).
   Never commit the `.p8`.
7. Actions › **iOS – upload to TestFlight** › Run workflow, with a build number higher than the last one (start at 3).
   Signing certificates and profiles are created automatically by Xcode with the API key.
8. In TestFlight, add testers (the client first). When happy, fill the App Store listing and submit for review.

### App Store listing checklist

- Screenshots: 6.9" iPhone (from the CI simulator screenshots or a real device).
- Privacy policy URL: `https://foodtruckcentre.com.au/privacy-policy/`.
- Support URL: `https://foodtruckcentre.com.au/`.
- App Privacy (“nutrition label”): collected – Name, Email, Phone, Other user content (event details, messages),
  all **linked to the user**, used for **App Functionality** only; no tracking; no third-party SDKs or analytics.
- Account deletion: available in the app (My account › Delete my account) – required by Apple for apps with sign-up.
- Review notes: provide a demo customer login (create one in the app, and give the email/password only in
  App Store Connect's review information) and explain that enquiries go to Food Truck Centre by email.
- Export compliance: `ITSAppUsesNonExemptEncryption = NO` is already set (HTTPS only).
