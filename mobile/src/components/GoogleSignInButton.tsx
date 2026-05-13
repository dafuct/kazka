import { Alert } from 'react-native';
import * as Google from 'expo-auth-session/providers/google';
import { useEffect } from 'react';
import { ApiError } from '@kazka/shared';
import { authApi } from '@/src/api/auth';
import { saveTokens } from '@/src/secure/tokenStorage';
import { useAuthStore } from '@/src/stores/auth.store';
import { bootstrapEntitlements } from '@/src/iap/bootstrap';
import { Button } from './Button';
import { useTranslation } from 'react-i18next';

export function GoogleSignInButton() {
  const { t } = useTranslation();
  const [_, response, promptAsync] = Google.useIdTokenAuthRequest({
    iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
  });

  useEffect(() => {
    if (response?.type !== 'success') return;
    const idToken = response.params.id_token;
    if (!idToken) return;
    void (async () => {
      try {
        const res = await authApi.googleLogin(idToken);
        await saveTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken });
        useAuthStore.getState().signIn({
          user: res.user,
          accessToken: res.accessToken,
          refreshToken: res.refreshToken,
        });
        void bootstrapEntitlements();
      } catch (e) {
        const code = e instanceof ApiError ? (e.body.error as string) : 'NETWORK';
        Alert.alert('Google sign-in', code);
      }
    })();
  }, [response]);

  return <Button title={t('login.withGoogle')} variant="secondary" onPress={() => promptAsync()} />;
}
