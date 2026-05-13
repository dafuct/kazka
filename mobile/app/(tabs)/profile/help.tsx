import { Linking, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';

const LINKS: { id: 'about' | 'terms' | 'privacy'; url: string }[] = [
  { id: 'about', url: 'https://kazka.app/about' },
  { id: 'terms', url: 'https://kazka.app/terms' },
  { id: 'privacy', url: 'https://kazka.app/privacy' },
];

export default function HelpScreen() {
  const { t } = useTranslation();
  const router = useRouter();

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={12} accessibilityRole="button" accessibilityLabel={t('reader.close')}>
          <Text style={styles.close}>{t('reader.close')}</Text>
        </TouchableOpacity>
        <Text style={styles.title}>{t('profile.help')}</Text>
      </View>

      <View style={styles.list}>
        {LINKS.map((l) => (
          <TouchableOpacity
            key={l.id}
            onPress={() => void Linking.openURL(l.url)}
            style={styles.row}
            accessibilityRole="link"
            accessibilityLabel={t(`profile.helpLinks.${l.id}`)}
          >
            <Text style={styles.rowText}>{t(`profile.helpLinks.${l.id}`)}</Text>
            <Text style={styles.chevron}>›</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.version}>Kazkar v0.1.0</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: 'transparent' },
  content: { padding: 24, paddingTop: 64, paddingBottom: 48, gap: 24, minHeight: '100%' },
  header: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  close: { fontSize: 15, color: theme.colors.accent },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  list: { marginTop: 16, backgroundColor: theme.colors.cardBg, borderRadius: theme.scalars.radius, borderWidth: 1, borderColor: theme.colors.cardBorder, overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 16, borderBottomWidth: 1, borderBottomColor: theme.colors.cardBorder },
  rowText: { fontSize: theme.scalars.bodySize + 1, color: theme.colors.text },
  chevron: { fontSize: 20, color: theme.colors.textFaint },
  version: { fontSize: 12, color: theme.colors.textFaint, textAlign: 'center', marginTop: 24 },
}));
