// In development, point to your local backend (run `cd backend && ./gradlew bootRun`
// from another terminal). On a physical device, replace localhost with your
// Mac's LAN IP.
//
// In production (TestFlight / App Store), this MUST point to the real backend.
export const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
