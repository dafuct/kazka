import { Text, TouchableOpacity, View } from 'react-native';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { API_BASE_URL } from '@/src/api/config';
import type { Story } from '@/src/api/types';

interface Props {
  story: Story;
  onPress: () => void;
}

export function HeroCard({ story, onPress }: Props) {
  const { t } = useTranslation();
  const path = story.illustrationPathLight ?? story.illustrationPathDark;
  const uri = path ? `${API_BASE_URL}${path}` : undefined;

  return (
    <TouchableOpacity onPress={onPress} style={styles.container}>
      {uri ? (
        <Image source={{ uri }} style={styles.image} transition={200} contentFit="cover" />
      ) : (
        <View style={[styles.image, styles.placeholder]} />
      )}
      <LinearGradient
        colors={['transparent', 'rgba(0,0,0,0.7)']}
        style={styles.overlay}
      />
      <View style={styles.titleContainer}>
        <Text style={styles.subtitle}>{t('home.featuredLabel')}</Text>
        <Text style={styles.title} numberOfLines={2}>{story.title}</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: {
    height: 280,
    borderRadius: theme.scalars.radius,
    overflow: 'hidden',
    backgroundColor: theme.colors.cardBg,
    marginBottom: 24,
  },
  image: { width: '100%', height: '100%' },
  placeholder: { backgroundColor: theme.colors.surfaceDeep },
  overlay: { position: 'absolute', left: 0, right: 0, bottom: 0, height: '60%' },
  titleContainer: { position: 'absolute', left: 16, right: 16, bottom: 16, gap: 4 },
  subtitle: { color: '#fff', fontSize: 13, opacity: 0.85 },
  title: { color: '#fff', fontSize: 24, fontWeight: theme.scalars.titleWeight },
}));
