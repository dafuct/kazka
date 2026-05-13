import * as Notifications from 'expo-notifications';
import * as Localization from 'expo-localization';
import { devicesApi } from '@/src/api/devices';

/**
 * Best-effort registration: gets the APNs device token from expo-notifications,
 * posts it to /api/devices/register. Never throws — failures are silently logged.
 *
 * Called on app start (after auth) and again when permission is granted later
 * (the post-first-story prompt path).
 */
export async function registerPushToken(): Promise<void> {
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
  try {
    const { data: deviceToken } = await Notifications.getDevicePushTokenAsync();
    await devicesApi.unregister(deviceToken);
  } catch (e) {
    console.warn('Push token unregister failed:', e);
  }
}
