import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { ApiError } from '@kazka/shared';
import { Button } from '@/src/components/Button';
import { Input } from '@/src/components/Input';
import { authApi } from '@/src/api/auth';
import { saveTokens } from '@/src/secure/tokenStorage';
import { useAuthStore } from '@/src/stores/auth.store';
import { AppleSignInButton } from '@/src/components/AppleSignInButton';
import { GoogleSignInButton } from '@/src/components/GoogleSignInButton';
import { bootstrapEntitlements } from '@/src/iap/bootstrap';

export default function LoginScreen() {
  const { t } = useTranslation();
  const router = useRouter();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit() {
    setLoading(true);
    try {
      const res = await authApi.tokenLogin(email.trim(), password);
      await saveTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken });
      useAuthStore.getState().signIn({
        user: res.user,
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
      });
      void bootstrapEntitlements();
    } catch (e) {
      const code = e instanceof ApiError ? (e.body.error as string) : 'NETWORK';
      Alert.alert(t('login.title'), t(`errors.${code}`, { defaultValue: t('errors.ERROR') }));
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.container}>
      <View style={styles.inner}>
        <Text style={styles.title}>{t('login.title')}</Text>
        <Input
          placeholder={t('login.email')}
          accessibilityLabel={t('login.email')}
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          keyboardType="email-address"
          autoComplete="email"
        />
        <Input
          placeholder={t('login.password')}
          accessibilityLabel={t('login.password')}
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          autoComplete="password"
        />
        <Button title={t('login.submit')} onPress={onSubmit} loading={loading} />
        <Button title={t('login.forgot')} variant="secondary" onPress={() => router.push('/(auth)/forgot')} />
        <AppleSignInButton />
        <GoogleSignInButton />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  inner: { flex: 1, padding: 24, paddingTop: 96, gap: 16 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text, marginBottom: 16 },
}));
