import type { EntitlementDto } from '@/src/api/billing';

export function isPro(entitlements: EntitlementDto[] | undefined): boolean {
  if (!entitlements) return false;
  return entitlements.some(
    (e) => (e.state === 'ACTIVE' || e.state === 'GRACE') &&
           (!e.expiresAt || new Date(e.expiresAt).getTime() > Date.now())
  );
}
