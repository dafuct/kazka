import { useEffect, useState } from 'react';
import {
  initConnection,
  endConnection,
  fetchProducts,
  getAvailablePurchases,
  type ProductSubscription,
} from 'react-native-iap';
import { PRODUCT_IDS, type ProductId } from './products';
import { billingApi } from '@/src/api/billing';
import { useEntitlementStore } from '@/src/stores/entitlement.store';
import { buy } from './purchaseFlow';

export function useIap() {
  const [products, setProducts] = useState<ProductSubscription[]>([]);
  const [ready, setReady] = useState(false);
  const setEntitlements = useEntitlementStore((s) => s.setEntitlements);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        await initConnection();
        // fetchProducts returns FetchProductsResult — a union of Product[],
        // ProductSubscription[], mixed, or null. With type:'subs' the runtime
        // yields ProductSubscription[]; narrow by trusting the request type.
        const subs = await fetchProducts({
          skus: [...PRODUCT_IDS],
          type: 'subs',
        });
        const list = (subs ?? []) as ProductSubscription[];
        if (!cancelled) {
          setProducts(list);
          setReady(true);
        }
      } catch {
        // StoreKit unavailable (simulator without sandbox account, etc.)
        if (!cancelled) setReady(true);
      }
    })();
    return () => {
      cancelled = true;
      void endConnection();
    };
  }, []);

  const purchase = async (productId: ProductId) => {
    const next = await buy(productId);
    setEntitlements(next);
  };

  const restore = async () => {
    await getAvailablePurchases();
    const next = await billingApi.myEntitlements();
    setEntitlements(next);
  };

  return { products, ready, purchase, restore };
}
