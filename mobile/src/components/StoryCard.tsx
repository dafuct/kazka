import { Text, TouchableOpacity, View } from 'react-native';
import { Image } from 'expo-image';
import { StyleSheet } from 'react-native-unistyles';
import { API_BASE_URL } from '@/src/api/config';
import type { Story } from '@/src/api/types';

interface Props {
  story: Story;
  onPress: () => void;
  variant?: 'compact' | 'tile';
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('uk-UA', { day: 'numeric', month: 'long' });
}

export function StoryCard({ story, onPress, variant = 'tile' }: Props) {
  const compact = variant === 'compact';
  const illustrationPath = story.illustrationPathLight ?? story.illustrationPathDark;
  const illustrationUri = illustrationPath ? `${API_BASE_URL}${illustrationPath}` : undefined;

  return (
    <TouchableOpacity onPress={onPress} style={compact ? styles.compact : styles.tile}>
      {illustrationUri ? (
        <Image source={{ uri: illustrationUri }} style={compact ? styles.compactImage : styles.tileImage} transition={200} cachePolicy="disk" />
      ) : (
        <View style={[compact ? styles.compactImage : styles.tileImage, styles.imagePlaceholder]} />
      )}
      <View style={styles.body}>
        <Text style={styles.title} numberOfLines={2}>{story.title}</Text>
        <Text style={styles.meta}>{formatDate(story.createdAt)}</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create((theme) => ({
  tile: {
    backgroundColor: theme.colors.cardBg,
    borderRadius: theme.scalars.radius,
    borderWidth: 1,
    borderColor: theme.colors.cardBorder,
    overflow: 'hidden',
  },
  compact: {
    backgroundColor: theme.colors.cardBg,
    borderRadius: theme.scalars.radius,
    borderWidth: 1,
    borderColor: theme.colors.cardBorder,
    flexDirection: 'row',
    overflow: 'hidden',
  },
  tileImage: { width: '100%', aspectRatio: 1, backgroundColor: theme.colors.surface2 },
  compactImage: { width: 64, height: 64, backgroundColor: theme.colors.surface2 },
  imagePlaceholder: { backgroundColor: theme.colors.surface2 },
  body: { padding: 12, gap: 4, flex: 1 },
  title: { fontSize: theme.scalars.bodySize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  meta: { fontSize: 12, color: theme.colors.textFaint },
}));
