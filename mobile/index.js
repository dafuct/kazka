// JS entrypoint: configure Unistyles themes BEFORE expo-router/entry
// scans the route tree (which transitively imports every screen, and
// each screen's top-level `StyleSheet.create((theme) => ...)` requires
// the themes to be registered).
import './src/theme/unistyles.config';
import 'expo-router/entry';
