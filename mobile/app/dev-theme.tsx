import { useState } from 'react';
import { ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { StyleSheet, UnistylesRuntime, useUnistyles } from 'react-native-unistyles';
import { pickTheme, type Mode, type VisualStyle } from '@/src/theme/tokens';
import '@/src/theme/unistyles.config';

export default function DevThemeScreen() {
  const [style, setStyle] = useState<VisualStyle>('cozy');
  const [mode, setMode] = useState<Mode>('light');

  // Switch Unistyles to the picked theme. Unistyles' shadow registry
  // will re-render dependent components automatically.
  UnistylesRuntime.setTheme(pickTheme(style, mode));

  // useUnistyles subscribes the component to theme changes and gives
  // typed access to the active theme tokens for inline use.
  const { theme } = useUnistyles();

  return (
    <ScrollView style={styles.scroll}>
      <View style={styles.container}>
        <Text style={styles.title}>Kazkar — Theme Preview</Text>
        <Text style={styles.body}>{style} · {mode}</Text>

        <View style={styles.row}>
          {(['cozy', 'playful', 'immersive'] as const).map((s) => (
            <TouchableOpacity
              key={s}
              onPress={() => setStyle(s)}
              style={[styles.chip, style === s && styles.chipActive]}
            >
              <Text style={[styles.chipText, style === s && styles.chipTextActive]}>{s}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.row}>
          {(['light', 'dark'] as const).map((m) => (
            <TouchableOpacity
              key={m}
              onPress={() => setMode(m)}
              style={[styles.chip, mode === m && styles.chipActive]}
            >
              <Text style={[styles.chipText, mode === m && styles.chipTextActive]}>{m}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.swatchGrid}>
          {Object.entries(theme.colors).map(([name, color]) => (
            <View key={name} style={styles.swatchCol}>
              <View style={[styles.swatch, { backgroundColor: color }]} />
              <Text style={styles.swatchLabel}>{name}</Text>
            </View>
          ))}
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create((theme) => ({
  scroll: { backgroundColor: theme.colors.bg },
  container: { padding: 24, paddingTop: 64, gap: 16 },
  title: {
    fontSize: theme.scalars.headlineSize,
    fontWeight: theme.scalars.titleWeight,
    color: theme.colors.text,
  },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted },
  row: { flexDirection: 'row', gap: 8 },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: theme.scalars.radius / 2,
    backgroundColor: theme.colors.surface,
  },
  chipActive: { backgroundColor: theme.colors.accent },
  chipText: { color: theme.colors.text },
  chipTextActive: { color: '#fff' },
  swatchGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  swatchCol: { width: 90, alignItems: 'center', gap: 4 },
  swatch: {
    width: 60,
    height: 60,
    borderRadius: theme.scalars.radius / 2,
    borderWidth: 1,
    borderColor: theme.colors.cardBorder,
  },
  swatchLabel: { fontSize: 10, color: theme.colors.textFaint, textAlign: 'center' },
}));
