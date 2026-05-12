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
