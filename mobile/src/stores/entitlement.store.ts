import { create } from 'zustand';
import type { EntitlementDto } from '@/src/api/billing';

interface EntitlementState {
  entitlements: EntitlementDto[];
  setEntitlements: (e: EntitlementDto[]) => void;
  clear: () => void;
}

export const useEntitlementStore = create<EntitlementState>((set) => ({
  entitlements: [],
  setEntitlements: (e) => set({ entitlements: e }),
  clear: () => set({ entitlements: [] }),
}));
