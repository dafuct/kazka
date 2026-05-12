import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { Button } from '@/src/components/Button';
import { Particles } from '@/src/components/Particles';
import { useAuthStore } from '@/src/stores/auth.store';
import { useThemeStore } from '@/src/stores/theme.store';
import { authApi } from '@/src/api/auth';
import { clearTokens } from '@/src/secure/tokenStorage';
import type { VisualStyle } from '@/src/theme/tokens';

const STYLES: { value: VisualStyle; label: string }[] = [
  { value: 'cozy', label: 'Затишний' },
  { value: 'playful', label: 'Грайливий' },
  { value: 'immersive', label: 'Казковий' },
];

export default function ProfileScreen() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const visualStyle = useThemeStore((s) => s.visualStyle);
  const darkMode = useThemeStore((s) => s.darkMode);
  const setVisualStyle = useThemeStore((s) => s.setVisualStyle);
  const toggleDarkMode = useThemeStore((s) => s.toggleDarkMode);

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
    <View style={{ flex: 1 }}>
      <Particles />
      <ScrollView style={styles.container} contentContainerStyle={styles.content}>
        <Text style={styles.title}>{user?.displayName ?? ''}</Text>
        <Text style={styles.body}>{user?.email ?? ''}</Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{t('profile.styleLabel')}</Text>
          <View style={styles.row}>
            {STYLES.map((s) => (
              <TouchableOpacity
                key={s.value}
                onPress={() => setVisualStyle(s.value)}
                style={[styles.chip, visualStyle === s.value && styles.chipActive]}
              >
                <Text style={[styles.chipText, visualStyle === s.value && styles.chipTextActive]}>
                  {s.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{t('profile.themeLabel')}</Text>
          <TouchableOpacity
            onPress={toggleDarkMode}
            style={[styles.chip, darkMode && styles.chipActive]}
          >
            <Text style={[styles.chipText, darkMode && styles.chipTextActive]}>
              {darkMode ? t('profile.nightTheme') : t('profile.dayTheme')}
            </Text>
          </TouchableOpacity>
        </View>

        <View style={{ flex: 1 }} />

        <Button
          title={t('profile.signOut')}
          variant="secondary"
          onPress={() => Alert.alert(t('profile.signOut'), '', [
            { text: 'Cancel', style: 'cancel' },
            { text: t('profile.signOut'), style: 'destructive', onPress: signOut },
          ])}
        />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  content: { padding: 24, paddingTop: 96, gap: 16, minHeight: '100%' },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, marginTop: 4 },
  section: { gap: 8, marginTop: 24 },
  sectionTitle: { fontSize: 14, fontWeight: '600', color: theme.colors.textMuted, textTransform: 'uppercase' },
  row: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  chip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: theme.scalars.radius / 2, backgroundColor: theme.colors.surface, alignSelf: 'flex-start' },
  chipActive: { backgroundColor: theme.colors.accent },
  chipText: { color: theme.colors.text },
  chipTextActive: { color: '#fff' },
}));
