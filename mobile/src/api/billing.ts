import { apiClient } from './client';

export interface ProductDto {
  id: string;
  appleProductId: string;
  name: string;
  priceMicro: number;
  currency: string;
  period: string;
  tier: string;
}

export interface EntitlementDto {
  productAppleId: string;
  state: 'ACTIVE' | 'EXPIRED' | 'GRACE' | 'REFUNDED' | 'REVOKED';
  expiresAt: string | null;
}

export const billingApi = {
  listProducts: () => apiClient.get<ProductDto[]>('/api/billing/products'),
  myEntitlements: () => apiClient.get<EntitlementDto[]>('/api/billing/entitlements'),
  verify: (signedTransaction: string) =>
    apiClient.post<EntitlementDto[]>('/api/billing/iap/verify', { signedTransaction }),
};
