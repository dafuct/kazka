import { useState } from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useRouter } from 'expo-router';
import { StyleSheet } from 'react-native-unistyles';
import { useIap } from '@/src/iap/useIap';
import { useEntitlementStore } from '@/src/stores/entitlement.store';
import { isPro } from '@/src/iap/entitlement';
import type { ProductId } from '@/src/iap/products';

export default function SubscriptionScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const { products, ready, purchase, restore } = useIap();
  const entitlements = useEntitlementStore((s) => s.entitlements);
  const pro = isPro(entitlements);
  const [busy, setBusy] = useState<ProductId | null>(null);

  const onBuy = async (productId: ProductId) => {
    setBusy(productId);
    try {
      await purchase(productId);
    } catch {
      Alert.alert(t('paywall.errorGeneric'));
    } finally {
      setBusy(null);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12} accessibilityRole="button" accessibilityLabel={t('reader.close')}>
          <Text style={styles.close}>{t('reader.close')}</Text>
        </Pressable>
        <Text style={styles.title}>{t('paywall.title')}</Text>
      </View>

      <Text style={styles.tagline}>{t('paywall.tagline')}</Text>

      <View style={styles.perks}>
        {(['unlimited', 'illustrations', 'noWatermark', 'widget'] as const).map((k) => (
          <View key={k} style={styles.perkRow}>
            <Text style={styles.check}>✓</Text>
            <Text style={styles.perkText}>{t(`paywall.perks.${k}`)}</Text>
          </View>
        ))}
      </View>

      {pro ? (
        <View style={styles.activeBox}>
          <Text style={styles.activeText}>{t('paywall.active')}</Text>
        </View>
      ) : !ready ? (
        <Text style={styles.tagline}>{t('create.starting')}</Text>
      ) : (
        <>
          {products.map((p) => (
            <Pressable
              key={p.id}
              style={[styles.buyButton, busy === p.id && styles.buyButtonBusy]}
              disabled={busy !== null}
              onPress={() => onBuy(p.id as ProductId)}
              accessibilityRole="button"
              accessibilityLabel={`${p.title} — ${priceOf(p)}`}
            >
              <Text style={styles.buyTitle}>
                {p.title || (p.id === 'kazka_pro_yearly' ? t('paywall.yearly') : t('paywall.monthly'))}
              </Text>
              <Text style={styles.buyPrice}>{priceOf(p)}</Text>
            </Pressable>
          ))}
        </>
      )}

      <Pressable onPress={() => void restore()} style={styles.restoreBtn} accessibilityRole="button" accessibilityLabel={t('paywall.restore')}>
        <Text style={styles.restoreText}>{t('paywall.restore')}</Text>
      </Pressable>
    </ScrollView>
  );
}

// react-native-iap v15: ProductSubscription exposes displayPrice (and on iOS,
// a deprecated localizedPriceIOS). Try both keys and fall back to the id.
function priceOf(p: { displayPrice?: string; localizedPriceIOS?: string | null; id: string }): string {
  return p.displayPrice ?? p.localizedPriceIOS ?? p.id;
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: 'transparent' },
  content: { padding: 24, paddingTop: 64, paddingBottom: 48, gap: 16 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  close: { fontSize: 15, color: theme.colors.accent },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  tagline: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted, marginTop: 4 },
  perks: { gap: 8, marginVertical: 16 },
  perkRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  check: { fontSize: 18, color: theme.colors.accent },
  perkText: { color: theme.colors.text, fontSize: theme.scalars.bodySize },
  activeBox: { padding: 16, backgroundColor: theme.colors.surface, borderRadius: theme.scalars.radius },
  activeText: { color: theme.colors.text, fontSize: theme.scalars.bodySize, textAlign: 'center' },
  buyButton: {
    padding: 16, backgroundColor: theme.colors.accent, borderRadius: theme.scalars.radius,
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
  },
  buyButtonBusy: { opacity: 0.6 },
  buyTitle: { color: '#fff', fontWeight: theme.scalars.titleWeight, fontSize: theme.scalars.bodySize },
  buyPrice: { color: '#fff', fontWeight: theme.scalars.titleWeight, fontSize: theme.scalars.bodySize },
  restoreBtn: { alignSelf: 'center', padding: 12, marginTop: 8 },
  restoreText: { color: theme.colors.accent, fontSize: theme.scalars.bodySize },
}));
