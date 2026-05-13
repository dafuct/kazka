import { Pressable, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet, useUnistyles } from 'react-native-unistyles';

export function TabBarCenterButton() {
  const router = useRouter();
  const { t } = useTranslation();
  const { theme } = useUnistyles();

  return (
    <Pressable
      onPress={() => router.push('/create/step-age')}
      style={styles.outer}
      hitSlop={12}
      accessibilityRole="button"
      accessibilityLabel={t('a11y.createButton')}
    >
      <View style={[styles.inner, { backgroundColor: theme.colors.accent }]}>
        <View style={[styles.plusH, { backgroundColor: '#fff' }]} />
        <View style={[styles.plusV, { backgroundColor: '#fff' }]} />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create(() => ({
  outer: { width: 60, height: 60, alignItems: 'center', justifyContent: 'center', marginTop: -20 },
  inner: { width: 56, height: 56, borderRadius: 28, alignItems: 'center', justifyContent: 'center' },
  plusH: { position: 'absolute', width: 22, height: 2.5, borderRadius: 2 },
  plusV: { position: 'absolute', width: 2.5, height: 22, borderRadius: 2 },
}));
