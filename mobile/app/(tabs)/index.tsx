import { Text, View } from 'react-native';
import { StyleSheet } from 'react-native-unistyles';
import { useAuthStore } from '@/src/stores/auth.store';

export default function HomeScreen() {
  const user = useAuthStore((s) => s.user);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Привіт, {user?.displayName ?? 'друже'}!</Text>
      <Text style={styles.body}>This is the placeholder home screen. Real content comes in M4.</Text>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg, padding: 24, paddingTop: 96 },
  title: { fontSize: theme.scalars.headlineSize, fontWeight: theme.scalars.titleWeight, color: theme.colors.text, marginBottom: 8 },
  body: { fontSize: theme.scalars.bodySize, color: theme.colors.textMuted },
}));
