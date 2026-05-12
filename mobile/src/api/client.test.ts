import { useAuthStore } from '@/src/stores/auth.store';
import { saveTokens, clearTokens } from '@/src/secure/tokenStorage';
import { apiClient } from './client';

const ORIGINAL_FETCH = global.fetch;

beforeEach(async () => {
  useAuthStore.setState({ user: null, accessToken: null, refreshToken: null, status: 'unknown' });
  await clearTokens();
});

afterEach(() => {
  global.fetch = ORIGINAL_FETCH;
});

describe('apiClient', () => {
  it('attaches Bearer header when access token is present', async () => {
    await saveTokens({ accessToken: 'access1', refreshToken: 'refresh1' });
    useAuthStore.getState().signIn({
      user: { id: 'u1' } as any,
      accessToken: 'access1',
      refreshToken: 'refresh1',
    });

    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ ok: true }),
    });
    global.fetch = fetchMock as any;

    await apiClient.get('/api/auth/me');

    const callInit = fetchMock.mock.calls[0][1];
    expect(callInit.headers['Authorization']).toBe('Bearer access1');
  });

  it('does NOT attach Bearer header when no token', async () => {
    const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({}) });
    global.fetch = fetchMock as any;

    await apiClient.get('/api/auth/me');

    const callInit = fetchMock.mock.calls[0][1];
    expect(callInit.headers['Authorization']).toBeUndefined();
  });

  it('refreshes on 401 and retries the original request', async () => {
    await saveTokens({ accessToken: 'old', refreshToken: 'r-old' });
    useAuthStore.getState().signIn({
      user: { id: 'u1' } as any,
      accessToken: 'old',
      refreshToken: 'r-old',
    });

    const fetchMock = jest.fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ error: 'UNAUTHENTICATED' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          accessToken: 'new',
          refreshToken: 'r-new',
          accessExpiresInSeconds: 3600,
          user: { id: 'u1' },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ user: { id: 'u1' } }),
      });
    global.fetch = fetchMock as any;

    const result = await apiClient.get('/api/auth/me');

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(result).toEqual({ user: { id: 'u1' } });
    expect(useAuthStore.getState().accessToken).toBe('new');
    expect(useAuthStore.getState().refreshToken).toBe('r-new');
  });

  it('signs out when refresh fails', async () => {
    await saveTokens({ accessToken: 'old', refreshToken: 'r-old' });
    useAuthStore.getState().signIn({
      user: { id: 'u1' } as any,
      accessToken: 'old',
      refreshToken: 'r-old',
    });

    const fetchMock = jest.fn()
      .mockResolvedValueOnce({ ok: false, status: 401, json: async () => ({}) })
      .mockResolvedValueOnce({ ok: false, status: 401, json: async () => ({ error: 'INVALID_REFRESH_TOKEN' }) });
    global.fetch = fetchMock as any;

    await expect(apiClient.get('/api/auth/me')).rejects.toThrow();
    expect(useAuthStore.getState().status).toBe('unauthenticated');
    expect(useAuthStore.getState().accessToken).toBeNull();
  });
});
