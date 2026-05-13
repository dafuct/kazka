export const PRODUCT_IDS = ['kazka_pro_monthly', 'kazka_pro_yearly'] as const;
export type ProductId = typeof PRODUCT_IDS[number];

export const TIER = {
  FREE: 'free',
  PRO: 'pro',
} as const;
export type Tier = typeof TIER[keyof typeof TIER];
