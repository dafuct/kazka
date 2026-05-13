import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { Button } from '@/src/components/Button';

const AGE_GROUPS: { value: '3-5' | '6-8' | '9-12'; label: string }[] = [
  { value: '3-5', label: '3–5' },
  { value: '6-8', label: '6–8' },
  { value: '9-12', label: '9–12' },
];

export default function StepAgeScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const [ageGroup, setAgeGroup] = useState<'3-5' | '6-8' | '9-12'>('3-5');

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Pressable
          onPress={() => router.back()}
          hitSlop={12}
          accessibilityRole="button"
          accessibilityLabel={t('create.cancel')}
        >
          <Text style={styles.cancel}>{t('create.cancel')}</Text>
        </Pressable>
        <Text style={styles.stepLabel}>{t('create.stepN', { n: 1, total: 2 })}</Text>
        <View style={{ width: 80 }} />
      </View>

      <View style={styles.body}>
        <Text style={styles.title}>{t('create.ageTitle')}</Text>
        <View style={styles.row}>
          {AGE_GROUPS.map((a) => (
            <Pressable
              key={a.value}
              onPress={() => setAgeGroup(a.value)}
              style={[styles.chip, ageGroup === a.value && styles.chipActive]}
              accessibilityRole="button"
              accessibilityLabel={a.label}
              accessibilityState={{ selected: ageGroup === a.value }}
            >
              <Text style={[styles.chipText, ageGroup === a.value && styles.chipTextActive]}>
                {a.label}
              </Text>
            </Pressable>
          ))}
        </View>
      </View>

      <View style={styles.footer}>
        <Button title={t('create.next')} onPress={() => router.push({ pathname: '/create/step-world', params: { ageGroup } })} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingTop: 56, paddingBottom: 12 },
  cancel: { fontSize: 15, color: theme.colors.accent, width: 80 },
  stepLabel: { fontSize: 13, color: theme.colors.textMuted, fontWeight: '500' },
  body: { flex: 1, paddingHorizontal: 24, paddingTop: 48, gap: 24 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  row: { flexDirection: 'row', gap: 12 },
  chip: { flex: 1, paddingVertical: 24, borderRadius: theme.scalars.radius, backgroundColor: theme.colors.surface, alignItems: 'center' },
  chipActive: { backgroundColor: theme.colors.accent },
  chipText: { fontSize: 24, color: theme.colors.text, fontWeight: theme.scalars.titleWeight },
  chipTextActive: { color: '#fff' },
  footer: { padding: 24, paddingBottom: 40 },
}));
