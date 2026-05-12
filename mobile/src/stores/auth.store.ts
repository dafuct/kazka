import { create } from 'zustand';
import type { User } from '@kazka/shared';

export type AuthStatus = 'unknown' | 'authenticated' | 'unauthenticated';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  status: AuthStatus;

  setStatus: (status: AuthStatus) => void;
  signIn: (args: { user: User; accessToken: string; refreshToken: string }) => void;
  refreshTokens: (args: { accessToken: string; refreshToken: string }) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  status: 'unknown',

  setStatus: (status) => set({ status }),

  signIn: ({ user, accessToken, refreshToken }) =>
    set({ user, accessToken, refreshToken, status: 'authenticated' }),

  refreshTokens: ({ accessToken, refreshToken }) =>
    set({ accessToken, refreshToken }),

  signOut: () =>
    set({ user: null, accessToken: null, refreshToken: null, status: 'unauthenticated' }),
}));
