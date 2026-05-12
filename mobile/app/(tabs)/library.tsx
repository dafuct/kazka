import { Text, View } from 'react-native';
import { StyleSheet } from 'react-native-unistyles';

export default function LibraryScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Бібліотека</Text>
      <Text style={styles.body}>Coming in M4.</Text>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg, padding: 24, paddingTop: 96 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted },
}));
