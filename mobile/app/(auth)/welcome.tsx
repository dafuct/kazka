import { Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';

export default function WelcomeScreen() {
  const { t } = useTranslation();
  const router = useRouter();

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.title}>{t('welcome.title')}</Text>
        <Text style={styles.subtitle}>{t('welcome.subtitle')}</Text>
      </View>

      <View style={styles.actions}>
        <TouchableOpacity style={styles.primaryButton} onPress={() => router.push('/(auth)/signup')}>
          <Text style={styles.primaryButtonText}>{t('welcome.signUp')}</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.secondaryButton} onPress={() => router.push('/(auth)/login')}>
          <Text style={styles.secondaryButtonText}>{t('welcome.signIn')}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: {
    flex: 1,
    backgroundColor: theme.colors.bg,
    paddingHorizontal: 24,
    paddingTop: 96,
    paddingBottom: 48,
    justifyContent: 'space-between',
  },
  hero: { alignItems: 'center', gap: 8 },
  title: { fontSize: 48, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  subtitle: { fontSize: 18, color: theme.colors.textMuted },
  actions: { gap: 12 },
  primaryButton: {
    backgroundColor: theme.colors.accent,
    borderRadius: theme.scalars.radius,
    paddingVertical: 16,
    alignItems: 'center',
  },
  primaryButtonText: { color: '#fff', fontSize: 17, fontWeight: '600' },
  secondaryButton: {
    backgroundColor: theme.colors.surface,
    borderRadius: theme.scalars.radius,
    paddingVertical: 16,
    alignItems: 'center',
  },
  secondaryButtonText: { color: theme.colors.text, fontSize: 17, fontWeight: '600' },
}));
