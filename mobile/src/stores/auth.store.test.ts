import { useAuthStore } from './auth.store';

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, accessToken: null, refreshToken: null, status: 'unknown' });
  });

  it('starts in unknown status with no user', () => {
    const state = useAuthStore.getState();
    expect(state.status).toBe('unknown');
    expect(state.user).toBeNull();
  });

  it('signIn populates user + tokens + status authenticated', () => {
    useAuthStore.getState().signIn({
      user: { id: 'u1', email: 'a@b.com' } as any,
      accessToken: 'a',
      refreshToken: 'r',
    });
    const state = useAuthStore.getState();
    expect(state.status).toBe('authenticated');
    expect(state.user?.id).toBe('u1');
    expect(state.accessToken).toBe('a');
    expect(state.refreshToken).toBe('r');
  });

  it('signOut clears user + tokens, sets status unauthenticated', () => {
    useAuthStore.getState().signIn({
      user: { id: 'u1', email: 'a@b.com' } as any,
      accessToken: 'a',
      refreshToken: 'r',
    });
    useAuthStore.getState().signOut();
    const state = useAuthStore.getState();
    expect(state.status).toBe('unauthenticated');
    expect(state.user).toBeNull();
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
  });

  it('refreshTokens updates both tokens without changing user', () => {
    useAuthStore.getState().signIn({
      user: { id: 'u1', email: 'a@b.com' } as any,
      accessToken: 'a-old',
      refreshToken: 'r-old',
    });
    useAuthStore.getState().refreshTokens({ accessToken: 'a-new', refreshToken: 'r-new' });
    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('a-new');
    expect(state.refreshToken).toBe('r-new');
    expect(state.user?.id).toBe('u1');
  });
});
