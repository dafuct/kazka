import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { ApiError } from '@kazka/shared';
import { Button } from '@/src/components/Button';
import { Input } from '@/src/components/Input';
import { authApi } from '@/src/api/auth';
import { useRouter } from 'expo-router';

export default function ForgotScreen() {
  const { t } = useTranslation();
  const router = useRouter();

  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit() {
    setLoading(true);
    try {
      await authApi.requestPasswordReset(email.trim());
      Alert.alert(t('forgot.title'), t('forgot.sent'), [
        { text: 'OK', onPress: () => router.replace('/(auth)/login') },
      ]);
    } catch (e) {
      const code = e instanceof ApiError ? (e.body.error as string) : 'NETWORK';
      Alert.alert(t('forgot.title'), t(`errors.${code}`, { defaultValue: t('errors.ERROR') }));
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.container}>
      <View style={styles.inner}>
        <Text style={styles.title}>{t('forgot.title')}</Text>
        <Text style={styles.body}>{t('forgot.body')}</Text>
        <Input
          placeholder={t('forgot.email')}
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          keyboardType="email-address"
        />
        <Button title={t('forgot.submit')} onPress={onSubmit} loading={loading} />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  inner: { flex: 1, padding: 24, paddingTop: 96, gap: 16 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, lineHeight: 22, marginBottom: 16 },
}));
