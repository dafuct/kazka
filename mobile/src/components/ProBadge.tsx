import { Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';

export function ProBadge() {
  const { t } = useTranslation();
  return (
    <View style={styles.badge} accessible accessibilityLabel={t('entitlement.proBadge')}>
      <Text style={styles.text}>{t('entitlement.pro')}</Text>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  badge: {
    paddingHorizontal: 8, paddingVertical: 3, borderRadius: 8,
    backgroundColor: theme.colors.accent,
    position: 'absolute', top: 12, right: 12,
  },
  text: { color: '#fff', fontSize: 11, fontWeight: theme.scalars.titleWeight },
}));
