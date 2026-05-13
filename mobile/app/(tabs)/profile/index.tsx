import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { Button } from '@/src/components/Button';
import { useAuthStore } from '@/src/stores/auth.store';
import { authApi } from '@/src/api/auth';
import { clearTokens } from '@/src/secure/tokenStorage';
import { unregisterPushToken } from '@/src/push/register';
import { clearEntitlements } from '@/src/iap/bootstrap';

export default function ProfileScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  async function signOut() {
    try { await unregisterPushToken(); } catch { /* best-effort */ }
    try { if (refreshToken) await authApi.tokenLogout(refreshToken); } catch { /* best-effort */ }
    await clearTokens();
    useAuthStore.getState().signOut();
    clearEntitlements();
  }

  const links: { id: 'account' | 'settings' | 'subscription' | 'help'; label: string }[] = [
    { id: 'account', label: t('profile.account') },
    { id: 'settings', label: t('profile.settings') },
    { id: 'subscription', label: t('paywall.title') },
    { id: 'help', label: t('profile.help') },
  ];

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>{user?.displayName ?? ''}</Text>
      <Text style={styles.body}>{user?.email ?? ''}</Text>

      <View style={styles.list}>
        {links.map((l) => (
          <TouchableOpacity
            key={l.id}
            onPress={() => router.push(`/(tabs)/profile/${l.id}`)}
            style={styles.row}
            accessibilityRole="button"
            accessibilityLabel={l.label}
          >
            <Text style={styles.rowText}>{l.label}</Text>
            <Text style={styles.chevron}>›</Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={{ flex: 1 }} />

      <Button
        title={t('profile.signOut')}
        variant="secondary"
        onPress={() => Alert.alert(t('profile.signOut'), '', [
          { text: t('reader.cancel'), style: 'cancel' },
          { text: t('profile.signOut'), style: 'destructive', onPress: signOut },
        ])}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: 'transparent' },
  content: { padding: 24, paddingTop: 96, gap: 16, minHeight: '100%' },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, marginTop: 4 },
  list: { marginTop: 32, backgroundColor: theme.colors.cardBg, borderRadius: theme.scalars.radius, borderWidth: 1, borderColor: theme.colors.cardBorder, overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 16, borderBottomWidth: 1, borderBottomColor: theme.colors.cardBorder },
  rowText: { fontSize: theme.scalars.bodySize + 1, color: theme.colors.text },
  chevron: { fontSize: 20, color: theme.colors.textFaint },
}));
