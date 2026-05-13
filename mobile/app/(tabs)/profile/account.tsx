import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { Button } from '@/src/components/Button';
import { useAuthStore } from '@/src/stores/auth.store';
import { authApi } from '@/src/api/auth';
import { clearTokens } from '@/src/secure/tokenStorage';
import { unregisterPushToken } from '@/src/push/register';

export default function AccountScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  async function localWipe() {
    try { await unregisterPushToken(); } catch { /* best-effort */ }
    try { if (refreshToken) await authApi.tokenLogout(refreshToken); } catch { /* best-effort */ }
    await clearTokens();
    useAuthStore.getState().signOut();
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={12} accessibilityRole="button" accessibilityLabel={t('reader.close')}>
          <Text style={styles.close}>{t('reader.close')}</Text>
        </TouchableOpacity>
        <Text style={styles.title}>{t('profile.account')}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.fieldLabel}>{t('login.email')}</Text>
        <Text style={styles.fieldValue}>{user?.email ?? ''}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.fieldLabel}>{t('signup.displayName')}</Text>
        <Text style={styles.fieldValue}>{user?.displayName ?? ''}</Text>
      </View>

      <View style={{ flex: 1 }} />

      <Button
        title={t('profile.deleteAccount')}
        variant="secondary"
        onPress={() => Alert.alert(t('profile.deleteAccount'), t('profile.deleteAccountConfirm'), [
          { text: t('reader.cancel'), style: 'cancel' },
          { text: t('profile.deleteAccount'), style: 'destructive', onPress: localWipe },
        ])}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: 'transparent' },
  content: { padding: 24, paddingTop: 64, paddingBottom: 48, gap: 24, minHeight: '100%' },
  header: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  close: { fontSize: 15, color: theme.colors.accent },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  section: { gap: 4 },
  fieldLabel: { fontSize: 13, color: theme.colors.textMuted, textTransform: 'uppercase', fontWeight: '600' },
  fieldValue: { fontSize: theme.scalars.bodySize + 2, color: theme.colors.text },
}));
