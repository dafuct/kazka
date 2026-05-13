import { Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { useNetworkStatus } from './useNetworkStatus';

export function OfflineBadge() {
  const { t } = useTranslation();
  const { isOnline } = useNetworkStatus();
  if (isOnline) return null;
  return (
    <View style={styles.badge}>
      <Text style={styles.text}>{t('offline.badge')}</Text>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  badge: {
    backgroundColor: theme.colors.surface2,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
    marginLeft: 24,
    marginTop: 8,
  },
  text: { fontSize: 11, color: theme.colors.textMuted, fontWeight: '600', textTransform: 'uppercase' },
}));
