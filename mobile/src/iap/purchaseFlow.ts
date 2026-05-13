import { finishTransaction, requestPurchase, type Purchase } from 'react-native-iap';
import { billingApi, type EntitlementDto } from '@/src/api/billing';
import type { ProductId } from './products';

/**
 * Pick the first Purchase from the polymorphic requestPurchase return shape
 * (Purchase | Purchase[] | null in react-native-iap v15).
 */
function firstPurchase(result: Purchase | Purchase[] | null): Purchase | null {
  if (!result) return null;
  return Array.isArray(result) ? result[0] ?? null : result;
}

/** Returns the new entitlement list after a successful purchase + verify. */
export async function buy(productId: ProductId): Promise<EntitlementDto[]> {
  const result = await requestPurchase({
    type: 'subs',
    request: {
      apple: { sku: productId },
      ios: { sku: productId },
    },
  });
  const purchase = firstPurchase(result);
  if (!purchase) {
    throw new Error('No purchase returned from StoreKit');
  }
  // On iOS in v15, purchase.purchaseToken is the StoreKit 2 JWS signed transaction.
  const jws = purchase.purchaseToken;
  if (!jws) {
    throw new Error('No signed transaction (JWS) from StoreKit');
  }
  const entitlements = await billingApi.verify(jws);
  await finishTransaction({ purchase, isConsumable: false });
  return entitlements;
}
