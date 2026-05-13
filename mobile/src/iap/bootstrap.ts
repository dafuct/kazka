import { billingApi } from '@/src/api/billing';
import { useEntitlementStore } from '@/src/stores/entitlement.store';

/** Fetch the user's entitlements and cache them. Safe to fire-and-forget. */
export async function bootstrapEntitlements(): Promise<void> {
  try {
    const e = await billingApi.myEntitlements();
    useEntitlementStore.getState().setEntitlements(e);
  } catch {
    useEntitlementStore.getState().setEntitlements([]);
  }
}

/** Wipe cached entitlements. Use on sign-out. */
export function clearEntitlements(): void {
  useEntitlementStore.getState().clear();
}
