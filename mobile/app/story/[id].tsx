import { useRef, useState } from 'react';
import { ActivityIndicator, Alert, FlatList, Pressable, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { PageView } from '@/src/components/PageView';
import { useStory } from '@/src/query/hooks';
import { shareLink } from '@/src/share/shareLink';
import { sharePdf } from '@/src/share/sharePdf';
import { useEntitlementStore } from '@/src/stores/entitlement.store';
import { isPro } from '@/src/iap/entitlement';

interface Page {
  text: string;
  illustrationPath?: string | null;
}

function splitPages(content: string): Page[] {
  // Split on blank lines (the backend uses \n\n between paragraphs).
  return content
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean)
    .map((text) => ({ text }));
}

export default function ReaderScreen() {
  const { t } = useTranslation();
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { data: story, isLoading } = useStory(id!);
  const [pageIndex, setPageIndex] = useState(0);
  const listRef = useRef<FlatList<Page>>(null);
  const entitlements = useEntitlementStore((s) => s.entitlements);
  const proUser = isPro(entitlements);

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator />
      </View>
    );
  }

  if (!story) {
    return (
      <View style={styles.center}>
        <Text style={styles.body}>{t('reader.loadFailed')}</Text>
      </View>
    );
  }

  const loadedStory = story;
  const pages = splitPages(loadedStory.content);
  const firstPage = pages[0];
  if (firstPage) {
    firstPage.illustrationPath =
      loadedStory.illustrationPathLight ?? loadedStory.illustrationPathDark ?? null;
  }

  function onShare() {
    Alert.alert(t('reader.shareTitle'), '', [
      { text: t('reader.cancel'), style: 'cancel' },
      { text: t('reader.shareLink'), onPress: () => shareLink(loadedStory.id, loadedStory.title) },
      { text: t('reader.sharePdf'), onPress: () => sharePdf(loadedStory, { watermark: !proUser }) },
    ]);
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Pressable
          onPress={() => router.back()}
          hitSlop={12}
          accessibilityRole="button"
          accessibilityLabel={t('reader.close')}
        >
          <Text style={styles.headerText}>{t('reader.close')}</Text>
        </Pressable>
        <Text style={styles.title} numberOfLines={1}>{loadedStory.title}</Text>
        <Pressable
          onPress={onShare}
          hitSlop={12}
          accessibilityRole="button"
          accessibilityLabel={t('reader.share')}
        >
          <Text style={styles.headerText}>{t('reader.share')}</Text>
        </Pressable>
      </View>

      <FlatList
        ref={listRef}
        data={pages}
        keyExtractor={(_, i) => `page-${i}`}
        renderItem={({ item }) => <PageView text={item.text} illustrationPath={item.illustrationPath} />}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onMomentumScrollEnd={(e) => {
          const w = e.nativeEvent.layoutMeasurement.width;
          setPageIndex(Math.round(e.nativeEvent.contentOffset.x / w));
        }}
      />

      <View style={styles.footer}>
        <Text style={styles.pageIndicator}>{pageIndex + 1} / {pages.length}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: theme.colors.bg },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 16, paddingTop: 56, paddingBottom: 12, gap: 12,
  },
  title: { flex: 1, textAlign: 'center', fontSize: 16, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  headerText: { fontSize: 15, color: theme.colors.accent },
  footer: { paddingBottom: 32, paddingTop: 12, alignItems: 'center' },
  pageIndicator: { fontSize: 13, color: theme.colors.textMuted },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted },
}));
