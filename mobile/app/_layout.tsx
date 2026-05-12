import { useEffect } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { Stack, useRouter, useSegments } from 'expo-router';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { useAuthStore } from '@/src/stores/auth.store';
import { readTokens } from '@/src/secure/tokenStorage';
import { authApi } from '@/src/api/auth';
import { queryClient, queryPersister } from '@/src/query/client';
import '@/src/theme/unistyles.config';
import { i18n } from '@/src/i18n';

void i18n;  // ensure i18n is initialised

export default function RootLayout() {
  const status = useAuthStore((s) => s.status);
  const segments = useSegments();
  const router = useRouter();

  // Bootstrap: read tokens from Keychain → hydrate store → call /me to verify
  useEffect(() => {
    void (async () => {
      const tokens = await readTokens();
      if (!tokens) {
        useAuthStore.getState().setStatus('unauthenticated');
        return;
      }
      useAuthStore.setState({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      });
      try {
        const res = await authApi.me();
        useAuthStore.getState().signIn({
          user: res.user,
          accessToken: useAuthStore.getState().accessToken!,
          refreshToken: useAuthStore.getState().refreshToken!,
        });
      } catch {
        useAuthStore.getState().signOut();
      }
    })();
  }, []);

  // Route gate
  useEffect(() => {
    if (status === 'unknown') return;
    const inAuthGroup = segments[0] === '(auth)';
    if (status === 'authenticated' && inAuthGroup) {
      router.replace('/(tabs)');
    } else if (status === 'unauthenticated' && !inAuthGroup) {
      router.replace('/(auth)/welcome');
    }
  }, [status, segments]);

  if (status === 'unknown') {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator />
      </View>
    );
  }

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister: queryPersister, maxAge: 24 * 60 * 60 * 1000 }}
    >
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="(auth)" options={{ headerShown: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      </Stack>
    </PersistQueryClientProvider>
  );
}
