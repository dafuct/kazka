import { useEffect, useState } from 'react';
import { Alert, Text, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { ApiError } from '@kazka/shared';
import { authApi } from '@/src/api/auth';
import { saveTokens } from '@/src/secure/tokenStorage';
import { useAuthStore } from '@/src/stores/auth.store';

export default function VerifyEmailScreen() {
  const { t } = useTranslation();
  const { email, token } = useLocalSearchParams<{ email?: string; token?: string }>();

  const [verifying, setVerifying] = useState(false);

  useEffect(() => {
    if (token) {
      void verify(token);
    }
  }, [token]);

  async function verify(t: string) {
    setVerifying(true);
    try {
      const res = await authApi.verifyEmail(t);
      await saveTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken });
      useAuthStore.getState().signIn({
        user: res.user,
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
      });
    } catch (e) {
      const code = e instanceof ApiError ? (e.body.error as string) : 'NETWORK';
      Alert.alert('Verify email', `${code}`);
    } finally {
      setVerifying(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{t('verifyEmail.title')}</Text>
      <Text style={styles.body}>{t('verifyEmail.body', { email: email ?? '' })}</Text>
      {verifying && <Text style={styles.body}>...</Text>}
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg, padding: 24, paddingTop: 96, gap: 16 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, lineHeight: 22 },
}));
