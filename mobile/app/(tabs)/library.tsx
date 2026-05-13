import { useCallback } from 'react';
import { FlatList, RefreshControl, Text, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet, useUnistyles } from 'react-native-unistyles';
import { Particles } from '@/src/components/Particles';
import { StoryCard } from '@/src/components/StoryCard';
import { OfflineBadge } from '@/src/network/OfflineBadge';
import { useStoriesInfinite } from '@/src/query/hooks';
import { writeTodayJSON } from '@/src/widget/appGroup';

export default function LibraryScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const { theme } = useUnistyles();
  const query = useStoriesInfinite(20);
  const items = query.data?.pages.flatMap((p) => p.items) ?? [];
  const latest = items[0] ?? null;

  useFocusEffect(
    useCallback(() => {
      if (latest) {
        void writeTodayJSON({
          title: latest.title,
          snippet: (latest.content ?? '').slice(0, 140),
          storyId: latest.id,
        });
      }
    }, [latest?.id, latest?.title, latest?.content])
  );

  return (
    <View style={styles.container}>
      <OfflineBadge />
      <Particles />
      <Text style={styles.title}>{t('library.title')}</Text>
      <FlatList
        data={items}
        keyExtractor={(s) => s.id}
        numColumns={2}
        columnWrapperStyle={styles.row}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <View style={styles.cell}>
            <StoryCard story={item} onPress={() => router.push(`/story/${item.id}`)} variant="tile" />
          </View>
        )}
        onEndReached={() => {
          if (query.hasNextPage && !query.isFetchingNextPage) query.fetchNextPage();
        }}
        onEndReachedThreshold={0.5}
        refreshControl={<RefreshControl refreshing={query.isRefetching} onRefresh={() => query.refetch()} tintColor={theme.colors.accent} />}
        ListEmptyComponent={
          !query.isLoading ? (
            <Text style={styles.empty}>{t('library.empty')}</Text>
          ) : null
        }
      />
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg, paddingHorizontal: 16, paddingTop: 64 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text, marginBottom: 16, paddingHorizontal: 8 },
  list: { gap: 12, paddingBottom: 100 },
  row: { gap: 12 },
  cell: { flex: 1, maxWidth: '49%' },
  empty: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, textAlign: 'center', marginTop: 48 },
}));
