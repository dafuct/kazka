import * as Notifications from 'expo-notifications';
import * as Localization from 'expo-localization';
import Constants from 'expo-constants';
import { devicesApi } from '@/src/api/devices';

// iOS simulators can't receive push notifications and won't have a valid
// `aps-environment` entitlement, so APNs token calls always fail there.
// Skip silently — keeps the dev console clean.
const isIosSimulator =
  (Constants.executionEnvironment as string | undefined) === 'storeClient'
    ? false
    : (Constants.platform as { ios?: { simulator?: boolean } } | undefined)?.ios?.simulator === true;

/**
 * Best-effort registration: gets the APNs device token from expo-notifications,
 * posts it to /api/devices/register. Never throws — failures are silently logged.
 *
 * Called on app start (after auth) and again when permission is granted later
 * (the post-first-story prompt path).
 */
export async function registerPushToken(): Promise<void> {
  if (isIosSimulator) return;
  const settings = await Notifications.getPermissionsAsync();
  if (settings.status !== 'granted') {
    // No prompt at boot — that's a later call from permission.ts.
    return;
  }
  try {
    const { data: deviceToken } = await Notifications.getDevicePushTokenAsync();
    const locale = Localization.getLocales()[0]?.languageCode ?? 'en';
    await devicesApi.register({ deviceToken, platform: 'ios', locale });
  } catch (e) {
    console.warn('Push token registration failed:', e);
  }
}

/**
 * Unregisters the current device token on sign-out. Best-effort.
 */
export async function unregisterPushToken(): Promise<void> {
  if (isIosSimulator) return;
  try {
    const { data: deviceToken } = await Notifications.getDevicePushTokenAsync();
    await devicesApi.unregister(deviceToken);
  } catch (e) {
    console.warn('Push token unregister failed:', e);
  }
}
