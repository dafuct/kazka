import { ApiError, type ApiErrorBody } from '@kazka/shared';
import { useAuthStore } from '@/src/stores/auth.store';
import { clearTokens, saveTokens } from '@/src/secure/tokenStorage';
import { API_BASE_URL } from './config';

interface RequestOptions {
  body?: unknown;
  headers?: Record<string, string>;
  skipRefreshOn401?: boolean;
}

// Single in-flight refresh promise — multiple concurrent 401s share one refresh.
let inFlightRefresh: Promise<void> | null = null;

async function performRefresh(): Promise<void> {
  if (inFlightRefresh) return inFlightRefresh;
  inFlightRefresh = (async () => {
    const refreshToken = useAuthStore.getState().refreshToken;
    if (!refreshToken) throw new Error('No refresh token available');

    const res = await fetch(`${API_BASE_URL}/api/auth/token/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) {
      await clearTokens();
      useAuthStore.getState().signOut();
      const body = (await res.json().catch(() => ({}))) as ApiErrorBody;
      throw new ApiError(res.status, body);
    }

    const tokens = await res.json();
    await saveTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
    useAuthStore.getState().refreshTokens({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
    });
  })().finally(() => {
    inFlightRefresh = null;
  });
  return inFlightRefresh;
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const accessToken = useAuthStore.getState().accessToken;
  const headers: Record<string, string> = {
    ...(options.body != null ? { 'Content-Type': 'application/json' } : {}),
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: options.body != null ? JSON.stringify(options.body) : undefined,
  });

  if (res.status === 401 && !options.skipRefreshOn401 && accessToken != null) {
    await performRefresh();
    return request<T>(method, path, { ...options, skipRefreshOn401: true });
  }

  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as ApiErrorBody;
    throw new ApiError(res.status, body);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, { ...options, body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, { ...options, body }),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, options),
};
