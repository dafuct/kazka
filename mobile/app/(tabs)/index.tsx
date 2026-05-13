import { ScrollView, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { HeroCard } from '@/src/components/HeroCard';
import { Particles } from '@/src/components/Particles';
import { StoryCard } from '@/src/components/StoryCard';
import { OfflineBadge } from '@/src/network/OfflineBadge';
import { useFeatured, useStoriesInfinite } from '@/src/query/hooks';
import { useAuthStore } from '@/src/stores/auth.store';

export default function HomeScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const featured = useFeatured();
  const stories = useStoriesInfinite(6);  // first page = 6 recents

  const recents = stories.data?.pages.flatMap((p) => p.items) ?? [];

  function open(id: string) {
    router.push(`/story/${id}`);
  }

  return (
    <View style={{ flex: 1 }}>
      <OfflineBadge />
      <Particles />
      <ScrollView style={styles.container} contentContainerStyle={styles.content}>
        <Text style={styles.greeting}>{t('home.greeting', { name: user?.displayName ?? 'друже' })}</Text>

        {featured.data && <HeroCard story={featured.data} onPress={() => open(featured.data!.id)} />}

        {recents.length > 0 && (
          <View>
            <Text style={styles.sectionTitle}>{t('home.recents')}</Text>
            <View style={styles.list}>
              {recents.map((s) => (
                <StoryCard key={s.id} story={s} onPress={() => open(s.id)} variant="compact" />
              ))}
            </View>
          </View>
        )}

        {!featured.isLoading && !featured.data && recents.length === 0 && (
          <Text style={styles.body}>{t('home.empty')}</Text>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  content: { padding: 24, paddingTop: 64, paddingBottom: 100, gap: 16 },
  greeting: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text, marginBottom: 8 },
  sectionTitle: { fontSize: 14, fontWeight: '600', color: theme.colors.textMuted, textTransform: 'uppercase', marginBottom: 12 },
  list: { gap: 8 },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted },
}));
