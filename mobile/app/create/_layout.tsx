import { Stack } from 'expo-router';

export default function CreateLayout() {
  return (
    <Stack screenOptions={{ presentation: 'modal', headerShown: false, animation: 'slide_from_bottom' }}>
      <Stack.Screen name="step-age" />
      <Stack.Screen name="step-world" />
      <Stack.Screen name="generating" />
    </Stack>
  );
}
