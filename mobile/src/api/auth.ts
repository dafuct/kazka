import type { AuthResponse, User } from '@kazka/shared';
import { apiClient } from './client';

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  accessExpiresInSeconds: number;
  user: User;
}

export const authApi = {
  signup: (email: string, password: string, displayName: string): Promise<TokenResponse> =>
    apiClient.post('/api/auth/signup', { email, password, displayName }),

  verifyEmail: (token: string): Promise<TokenResponse> =>
    apiClient.post('/api/auth/verify-email', { token }),

  resendVerification: (): Promise<void> =>
    apiClient.post('/api/auth/verify-email/resend'),

  tokenLogin: (email: string, password: string): Promise<TokenResponse> =>
    apiClient.post('/api/auth/token/login', { email, password }),

  tokenLogout: (refreshToken: string): Promise<void> =>
    apiClient.post('/api/auth/token/logout', { refreshToken }),

  appleLogin: (args: {
    identityToken: string;
    authorizationCode?: string;
    fullName?: string;
    email?: string;
  }): Promise<TokenResponse> =>
    apiClient.post('/api/auth/oauth/apple', args),

  googleLogin: (idToken: string): Promise<TokenResponse> =>
    apiClient.post('/api/auth/oauth/google', { idToken }),

  requestPasswordReset: (email: string): Promise<void> =>
    apiClient.post('/api/auth/password-reset/request', { email }),

  confirmPasswordReset: (token: string, newPassword: string): Promise<void> =>
    apiClient.post('/api/auth/password-reset/confirm', { token, newPassword }),

  me: (): Promise<AuthResponse> => apiClient.get('/api/auth/me'),
};
