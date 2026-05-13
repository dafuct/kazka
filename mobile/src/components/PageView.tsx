import { Dimensions, ScrollView, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { StyleSheet } from 'react-native-unistyles';
import { API_BASE_URL } from '@/src/api/config';
import { useEntitlementStore } from '@/src/stores/entitlement.store';
import { isPro } from '@/src/iap/entitlement';
import { ProBadge } from './ProBadge';

interface Props {
  text: string;
  illustrationPath?: string | null;
}

const { width } = Dimensions.get('window');

export function PageView({ text, illustrationPath }: Props) {
  const uri = illustrationPath ? `${API_BASE_URL}${illustrationPath}` : undefined;
  const entitlements = useEntitlementStore((s) => s.entitlements);
  const pro = isPro(entitlements);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      accessible
      accessibilityLabel={text}
    >
      {uri && (
        <View style={styles.illustrationWrap}>
          <Image source={{ uri }} style={styles.illustration} transition={200} contentFit="cover" cachePolicy="disk" />
          {!pro && <ProBadge />}
        </View>
      )}
      <Text style={styles.text}>{text}</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { width, flex: 1, backgroundColor: theme.colors.bg },
  content: { padding: 24, paddingTop: 80, paddingBottom: 80, gap: 24 },
  illustrationWrap: { position: 'relative' },
  illustration: { width: '100%', aspectRatio: 1, borderRadius: theme.scalars.radius },
  text: { fontSize: theme.scalars.bodySize + 2, color: theme.colors.text, lineHeight: 28 },
}));
