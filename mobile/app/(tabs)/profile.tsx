import { Alert, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { Button } from '@/src/components/Button';
import { useAuthStore } from '@/src/stores/auth.store';
import { authApi } from '@/src/api/auth';
import { clearTokens } from '@/src/secure/tokenStorage';

export default function ProfileScreen() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  async function signOut() {
    try {
      if (refreshToken) {
        await authApi.tokenLogout(refreshToken);
      }
    } catch {
      // Best-effort — proceed with local sign-out even if server fails.
    } finally {
      await clearTokens();
      useAuthStore.getState().signOut();
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{user?.displayName ?? ''}</Text>
      <Text style={styles.body}>{user?.email ?? ''}</Text>

      <View style={{ flex: 1 }} />

      <Button
        title={t('profile.signOut')}
        variant="secondary"
        onPress={() => Alert.alert(t('profile.signOut'), '', [
          { text: 'Cancel', style: 'cancel' },
          { text: t('profile.signOut'), style: 'destructive', onPress: signOut },
        ])}
      />
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg, padding: 24, paddingTop: 96 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, marginTop: 4 },
}));
