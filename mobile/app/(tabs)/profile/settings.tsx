import { ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { useThemeStore } from '@/src/stores/theme.store';
import type { VisualStyle } from '@/src/theme/tokens';

const STYLE_VALUES: VisualStyle[] = ['cozy', 'playful', 'immersive'];
const LANGUAGES: { code: 'uk' | 'en'; label: string }[] = [
  { code: 'uk', label: 'Українська' },
  { code: 'en', label: 'English' },
];

export default function SettingsScreen() {
  const { t, i18n: i18nInstance } = useTranslation();
  const router = useRouter();
  const { visualStyle, darkMode, setVisualStyle, toggleDarkMode } = useThemeStore();

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={12} accessibilityRole="button" accessibilityLabel={t('reader.close')}>
          <Text style={styles.close}>{t('reader.close')}</Text>
        </TouchableOpacity>
        <Text style={styles.title}>{t('profile.settings')}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>{t('profile.styleLabel')}</Text>
        <View style={styles.row}>
          {STYLE_VALUES.map((v) => (
            <TouchableOpacity
              key={v}
              onPress={() => setVisualStyle(v)}
              style={[styles.chip, visualStyle === v && styles.chipActive]}
              accessibilityRole="button"
              accessibilityLabel={t(`profile.styleLabels.${v}`)}
            >
              <Text style={[styles.chipText, visualStyle === v && styles.chipTextActive]}>
                {t(`profile.styleLabels.${v}`)}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>{t('profile.themeLabel')}</Text>
        <TouchableOpacity
          onPress={toggleDarkMode}
          style={[styles.chip, styles.chipStandalone, darkMode && styles.chipActive]}
          accessibilityRole="switch"
          accessibilityLabel={darkMode ? t('profile.nightTheme') : t('profile.dayTheme')}
          accessibilityState={{ checked: darkMode }}
        >
          <Text style={[styles.chipText, darkMode && styles.chipTextActive]}>
            {darkMode ? t('profile.nightTheme') : t('profile.dayTheme')}
          </Text>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>{t('profile.languageLabel')}</Text>
        <View style={styles.row}>
          {LANGUAGES.map((l) => (
            <TouchableOpacity
              key={l.code}
              onPress={() => { void i18nInstance.changeLanguage(l.code); }}
              style={[styles.chip, i18nInstance.language === l.code && styles.chipActive]}
              accessibilityRole="button"
              accessibilityLabel={l.label}
            >
              <Text style={[styles.chipText, i18nInstance.language === l.code && styles.chipTextActive]}>
                {l.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: 'transparent' },
  content: { padding: 24, paddingTop: 64, paddingBottom: 48, gap: 24, minHeight: '100%' },
  header: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  close: { fontSize: 15, color: theme.colors.accent },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  section: { gap: 8, marginTop: 16 },
  sectionTitle: { fontSize: 14, fontWeight: '600', color: theme.colors.textMuted, textTransform: 'uppercase' },
  row: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  chip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: theme.scalars.radius / 2, backgroundColor: theme.colors.surface },
  chipStandalone: { alignSelf: 'flex-start' },
  chipActive: { backgroundColor: theme.colors.accent },
  chipText: { color: theme.colors.text },
  chipTextActive: { color: '#fff' },
}));
