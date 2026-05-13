import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { ApiError } from '@kazka/shared';
import { Button } from '@/src/components/Button';
import { Input } from '@/src/components/Input';
import { authApi } from '@/src/api/auth';
import { AppleSignInButton } from '@/src/components/AppleSignInButton';
import { GoogleSignInButton } from '@/src/components/GoogleSignInButton';

export default function SignupScreen() {
  const { t } = useTranslation();
  const router = useRouter();

  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit() {
    if (password.length < 8) {
      Alert.alert(t('signup.title'), 'Password must be at least 8 characters.');
      return;
    }
    setLoading(true);
    try {
      await authApi.signup(email.trim(), password, displayName.trim());
      router.replace({ pathname: '/(auth)/verify-email', params: { email: email.trim() } });
    } catch (e) {
      const code = e instanceof ApiError ? (e.body.error as string) : 'NETWORK';
      Alert.alert(t('signup.title'), t(`errors.${code}`, { defaultValue: t('errors.ERROR') }));
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.container}>
      <View style={styles.inner}>
        <Text style={styles.title}>{t('signup.title')}</Text>
        <Input
          placeholder={t('signup.displayName')}
          accessibilityLabel={t('signup.displayName')}
          value={displayName}
          onChangeText={setDisplayName}
        />
        <Input
          placeholder={t('signup.email')}
          accessibilityLabel={t('signup.email')}
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          keyboardType="email-address"
        />
        <Input
          placeholder={t('signup.password')}
          accessibilityLabel={t('signup.password')}
          value={password}
          onChangeText={setPassword}
          secureTextEntry
        />
        <Button title={t('signup.submit')} onPress={onSubmit} loading={loading} />
        <Button title={t('signup.alreadyHave')} variant="secondary" onPress={() => router.replace('/(auth)/login')} />
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
