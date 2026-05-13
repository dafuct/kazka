# StoreKit + Widget Manual Verification (TestFlight #4)

## Prerequisites

- Backend deployed (or running locally on a tunnel reachable from the device)
- App Store Connect: subscription products `kazka_pro_monthly` + `kazka_pro_yearly` created and Ready to Submit
- App Store Connect → App Store Server Notifications V2: production URL set to `https://<your-host>/api/billing/iap/webhook`
- iOS Sandbox account: created at App Store Connect → Users and Access → Sandbox Testers
- Device: signed in to the sandbox account at Settings → App Store → Sandbox Account

## StoreKit purchase + verify

1. Build to device: `cd mobile && npx expo run:ios --device <UDID>`
2. Sign in to the app.
3. Profile → Kazka Pro → tap Monthly.
4. StoreKit sheet appears → confirm with the sandbox account.
5. Verify on backend:
   ```bash
   curl -H "Authorization: Bearer <accessToken>" \
        https://<host>/api/billing/entitlements
   ```
   Expected: `[{ "productAppleId": "kazka_pro_monthly", "state": "ACTIVE", ... }]`
6. Profile → Kazka Pro screen now shows "You are subscribed".
7. Open any story → no ProBadge on the illustration; sharePdf produces a PDF with no watermark footer.

## Restore purchases

8. Delete and reinstall the app.
9. Sign in. Profile → Kazka Pro → Restore purchases.
10. The same entitlement reappears in the store.

## Widget setup

11. Long-press an empty area of the home screen → + → search "Kazkar" → add small.
12. Widget shows placeholder ("Open the app to create your first story.") until first story is generated.
13. Generate a story in the app → swipe to widget → it shows the new title and snippet.
14. Tap the widget → the app opens directly on the story reader.

Deep-link sanity (Simulator):
```bash
xcrun simctl openurl booted "kazka://story/<id>"
```

## ASN V2 webhook (sandbox)

15. Set up an ngrok tunnel pointing at local backend: `ngrok http 8080`.
16. App Store Connect → App → Subscriptions → Test Notifications → enter `https://<ngrok>.ngrok.app/api/billing/iap/webhook` → V2.
17. Send a REFUND notification for the active sandbox transaction.
18. Confirm DB:
   ```sql
   SELECT state, original_transaction_id FROM user_entitlements
   WHERE original_transaction_id = '<txnId>';
   ```
   Expected: `REFUNDED`.

## Free-tier paywall enforcement

19. Sign in as a fresh user (not Pro).
20. Generate 3 stories. The 4th attempt should fail with a 402 PAYMENT_REQUIRED error.
21. (Optionally) verify in DB: `SELECT stories_this_month FROM users WHERE email = '<email>';` → 3.

## In-app push smoke

22. Once `pod install` works and the device is registered for push (Phase C of M5):
    ```bash
    bash mobile/scripts/test-push-simulator.sh <storyId>
    ```
23. Banner notification fires; tap → app opens on the story reader.

## TestFlight #4 — build & upload

After App Store Connect setup (subscriptions + ASN webhook URL) and the one-time Xcode widget-target add:

1. Bump `buildNumber` in `mobile/app.config.ts` if you haven't already (M6 lands at `4`).
2. Backend env vars must be set in production:
   ```
   BILLING_BUNDLE_ID=app.kazka.ios
   BILLING_APPLE_ID=<numeric App Store Connect app id>
   BILLING_ENVIRONMENT=Production    # or Sandbox for the first TestFlight cuts
   BILLING_ENABLED=true
   BILLING_FREE_LIMIT=3
   APNS_TEAM_ID=...
   APNS_KEY_ID=...
   APNS_PRIVATE_KEY_PEM="-----BEGIN PRIVATE KEY-----\n..."
   APNS_BUNDLE_ID=app.kazka.ios
   APNS_HOST=api.push.apple.com       # api.sandbox.push.apple.com for sandbox
   APNS_ENABLED=true
   ```
3. Build via EAS (recommended) or Xcode Organizer:
   ```bash
   cd mobile && eas build --platform ios --profile preview
   ```
   Or, archive locally:
   ```bash
   cd mobile && npx expo run:ios --configuration Release --no-bundler
   # Then in Xcode: Product → Archive → Window → Organizer → Distribute App → App Store Connect
   ```
4. Upload to TestFlight via Organizer or `eas submit`.
5. Add internal testers in App Store Connect.
6. Walk this playbook end-to-end on the TestFlight build.

## Known limitations (v1)

- The Xcode widget extension target must be added manually once per checkout (`File → New → Target → Widget Extension`, name `KazkarWidget`, attach the source files in `ios/KazkarWidget/`).
- The free-tier counter resets on the 1st of each UTC month via the `MonthlyCounterResetJob` cron (00:05 UTC daily).
- Apple webhook retry: a malformed signed payload is logged + returned 200 to suppress retries (see GlobalExceptionHandler / BillingController.webhook).
