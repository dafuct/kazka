import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { Button } from '@/src/components/Button';
import { Input } from '@/src/components/Input';

const PRESET_IDS = ['forest', 'sea', 'space', 'castle', 'mountains', 'village'] as const;
type PresetId = typeof PRESET_IDS[number];

export default function StepWorldScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const { ageGroup } = useLocalSearchParams<{ ageGroup: '3-5' | '6-8' | '9-12' }>();

  const [selectedId, setSelectedId] = useState<PresetId | null>(null);
  const [custom, setCustom] = useState('');

  const presets = PRESET_IDS.map((id) => ({ id, label: t(`create.worlds.${id}`) }));

  const theme = selectedId
    ? presets.find((p) => p.id === selectedId)?.label ?? ''
    : custom.trim();

  const canSubmit = theme.length > 0;

  function start() {
    router.replace({
      pathname: '/create/generating',
      params: {
        ageGroup: ageGroup ?? '3-5',
        theme,
      },
    });
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Text style={styles.cancel}>{t('create.back')}</Text>
        </Pressable>
        <Text style={styles.stepLabel}>{t('create.stepN', { n: 2, total: 2 })}</Text>
        <View style={{ width: 80 }} />
      </View>

      <ScrollView style={styles.body} contentContainerStyle={styles.bodyContent}>
        <Text style={styles.title}>{t('create.worldTitle')}</Text>
        <View style={styles.grid}>
          {presets.map((p) => (
            <Pressable
              key={p.id}
              onPress={() => { setSelectedId(p.id); setCustom(''); }}
              style={[styles.tile, selectedId === p.id && styles.tileActive]}
            >
              <Text style={[styles.tileText, selectedId === p.id && styles.tileTextActive]}>{p.label}</Text>
            </Pressable>
          ))}
        </View>
        <View style={styles.customRow}>
          <Text style={styles.label}>{t('create.customLabel')}</Text>
          <Input
            placeholder={t('create.customPlaceholder')}
            value={custom}
            onChangeText={(v) => { setCustom(v); if (v) setSelectedId(null); }}
            multiline
          />
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <Button title={t('create.submit')} onPress={start} disabled={!canSubmit} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingTop: 56, paddingBottom: 12 },
  cancel: { fontSize: 15, color: theme.colors.accent, width: 80 },
  stepLabel: { fontSize: 13, color: theme.colors.textMuted, fontWeight: '500' },
  body: { flex: 1 },
  bodyContent: { paddingHorizontal: 24, paddingTop: 32, gap: 24 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  tile: { width: '47%', paddingVertical: 24, paddingHorizontal: 12, borderRadius: theme.scalars.radius, backgroundColor: theme.colors.surface, alignItems: 'center' },
  tileActive: { backgroundColor: theme.colors.accent },
  tileText: { fontSize: 14, color: theme.colors.text, textAlign: 'center', fontWeight: '500' },
  tileTextActive: { color: '#fff' },
  customRow: { gap: 8 },
  label: { fontSize: 13, color: theme.colors.textMuted },
  footer: { padding: 24, paddingBottom: 40 },
}));
