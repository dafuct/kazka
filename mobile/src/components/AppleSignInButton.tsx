import { Alert } from 'react-native';
import * as AppleAuth from 'expo-apple-authentication';
import { useTranslation } from 'react-i18next';
import { ApiError } from '@kazka/shared';
import { authApi } from '@/src/api/auth';
import { saveTokens } from '@/src/secure/tokenStorage';
import { useAuthStore } from '@/src/stores/auth.store';
import { bootstrapEntitlements } from '@/src/iap/bootstrap';

interface Props { style?: any }

export function AppleSignInButton({ style }: Props) {
  const { t } = useTranslation();
  return (
    <AppleAuth.AppleAuthenticationButton
      buttonType={AppleAuth.AppleAuthenticationButtonType.SIGN_IN}
      buttonStyle={AppleAuth.AppleAuthenticationButtonStyle.BLACK}
      cornerRadius={20}
      style={[{ height: 50 }, style]}
      accessibilityLabel={t('a11y.appleSignIn')}
      onPress={async () => {
        try {
          const credential = await AppleAuth.signInAsync({
            requestedScopes: [
              AppleAuth.AppleAuthenticationScope.FULL_NAME,
              AppleAuth.AppleAuthenticationScope.EMAIL,
            ],
          });
          if (!credential.identityToken) {
            throw new Error('No identity token from Apple');
          }
          const fullName = credential.fullName
            ? [credential.fullName.givenName, credential.fullName.familyName].filter(Boolean).join(' ')
            : undefined;
          const res = await authApi.appleLogin({
            identityToken: credential.identityToken,
            authorizationCode: credential.authorizationCode ?? undefined,
            fullName,
            email: credential.email ?? undefined,
          });
          await saveTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken });
          useAuthStore.getState().signIn({
            user: res.user,
            accessToken: res.accessToken,
            refreshToken: res.refreshToken,
          });
          void bootstrapEntitlements();
        } catch (e) {
          if ((e as any)?.code === 'ERR_REQUEST_CANCELED') return;
          const code = e instanceof ApiError ? (e.body.error as string) : 'NETWORK';
          Alert.alert('Apple sign-in', code);
        }
      }}
    />
  );
}
