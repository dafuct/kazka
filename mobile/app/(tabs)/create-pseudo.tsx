import { Redirect } from 'expo-router';

// Pseudo-tab — taps are intercepted by tabBarButton/listeners in _layout.tsx and
// open the /create modal stack instead. This file exists only so Expo Router
// stops warning about the unmounted Tabs.Screen.
export default function CreatePseudoTab() {
  return <Redirect href="/create/step-age" />;
}
