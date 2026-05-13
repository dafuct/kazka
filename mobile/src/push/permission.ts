import * as Notifications from 'expo-notifications';
import { registerPushToken } from './register';

// MMKV usage matches Phase C of M4 (v4 API: createMMKV + .remove).
function getStore() {
  const mod = require('react-native-mmkv');
  return mod.createMMKV
    ? mod.createMMKV({ id: 'kazka.push.v1' })
    : new mod.MMKV({ id: 'kazka.push.v1' });
}

const PROMPTED_KEY = 'kazka.push.prompted';

/**
 * Request push notification permission. Idempotent — only prompts once per
 * device install (gated by the MMKV flag). Always re-checks current permission
 * status; if already granted, registers the token.
 */
export async function maybeRequestPushPermission(): Promise<void> {
  const store = getStore();
  const current = await Notifications.getPermissionsAsync();
  if (current.status === 'granted') {
    void registerPushToken();
    return;
  }
  if (store.getString(PROMPTED_KEY) === 'true') {
    return;  // already asked once; don't pester
  }
  const result = await Notifications.requestPermissionsAsync();
  store.set(PROMPTED_KEY, 'true');
  if (result.status === 'granted') {
    void registerPushToken();
  }
}
