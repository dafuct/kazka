import { clearTokens, readTokens, saveTokens } from './tokenStorage';

describe('tokenStorage', () => {
  it('saves and reads back both tokens', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r' });
    expect(await readTokens()).toEqual({ accessToken: 'a', refreshToken: 'r' });
  });

  it('returns null when no tokens stored', async () => {
    await clearTokens();
    expect(await readTokens()).toBeNull();
  });

  it('clearTokens removes both', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r' });
    await clearTokens();
    expect(await readTokens()).toBeNull();
  });
});
