# Kazkar Mobile

## OAuth setup (one-time)

### Google
1. Go to https://console.cloud.google.com/apis/credentials
2. Create an OAuth 2.0 Client ID for iOS with bundle id `app.kazka.ios`
3. Set the EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID env var (in `.env` for local dev,
   or in TestFlight's app secrets for prod):
   ```
   EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=xxxx-yyyy.apps.googleusercontent.com
   ```

### Apple
Configured via Xcode signing — see ios/ project settings after `expo prebuild`.

## TestFlight build

Prerequisites:
- Apple Developer Program membership ($99/year)
- App Store Connect app registered with bundle id `app.kazka.ios`
- Production backend reachable at an HTTPS URL — set `EXPO_PUBLIC_API_BASE_URL` accordingly

Steps:
1. From `mobile/ios/`, install CocoaPods dependencies (one-time per checkout, or after any native dep change):
   ```bash
   cd mobile/ios && pod install
   ```
   This creates `Kazkar.xcworkspace`. Open it in Xcode (always use the `.xcworkspace`, never the `.xcodeproj`, so CocoaPods are linked).
2. Set the signing team in `Kazkar > Targets > Kazkar > Signing & Capabilities`.
3. Configure release env: `EXPO_PUBLIC_API_BASE_URL=https://api.kazkatales.com` (or your prod URL).
4. Build for "Any iOS Device (arm64)" with the release scheme.
5. `Product > Archive`, then "Distribute App" → App Store Connect → Upload.
6. Wait for the build to appear in App Store Connect → assign to internal testers → TestFlight.

Bump `buildNumber` in `app.config.ts` for each new upload (App Store Connect rejects duplicate build numbers for the same version).
