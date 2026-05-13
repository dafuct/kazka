import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { StyleSheet } from 'react-native-unistyles';
import { openSseStream } from '@/src/api/sse';
import { queryKeys } from '@/src/query/keys';
import { maybeRequestPushPermission } from '@/src/push/permission';

export default function GeneratingScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const qc = useQueryClient();
  const { ageGroup, theme } = useLocalSearchParams<{ ageGroup: string; theme: string }>();
  const [phase, setPhase] = useState<'starting' | 'streaming' | 'done' | 'error'>('starting');
  const [error, setError] = useState<string | null>(null);
  const cancelRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (!ageGroup || !theme) {
      setError('Missing params');
      setPhase('error');
      return;
    }

    const { events, cancel } = openSseStream('/api/stories/generate', {
      theme,
      ageGroup,
      characters: ['Hero'],
      length: 'short',
      language: 'uk',
    });
    cancelRef.current = cancel;

    let cancelled = false;

    (async () => {
      let storyId: string | null = null;
      try {
        for await (const ev of events) {
          if (cancelled) break;
          if (ev.type === 'meta') {
            // The story id is also surfaced by `done`; we don't act on meta yet.
            continue;
          }
          if (ev.type === 'token') {
            setPhase('streaming');
          } else if (ev.type === 'done') {
            // Backend emits done with JSON payload { id, title }
            try {
              const parsed = JSON.parse(ev.data);
              if (parsed && typeof parsed === 'object' && typeof parsed.id === 'string') {
                storyId = parsed.id;
              } else if (typeof parsed === 'string') {
                storyId = parsed;
              }
            } catch {
              // Fallback: backend might emit the id as a plain string
              storyId = ev.data && ev.data.trim() ? ev.data.trim() : null;
            }
            setPhase('done');
            break;
          } else if (ev.type === 'error') {
            // Backend emits error with JSON payload { message } or { code }
            let message = ev.data || t('errors.ERROR');
            try {
              const parsed = JSON.parse(ev.data);
              if (parsed && typeof parsed === 'object') {
                message = parsed.message ?? parsed.code ?? message;
              }
            } catch {
              // leave message as-is
            }
            setError(message);
            setPhase('error');
            break;
          }
        }
      } catch (e) {
        if (!cancelled) {
          const message = e instanceof Error ? e.message : 'Network error';
          setError(message);
          setPhase('error');
        }
      }

      if (storyId && !cancelled) {
        void maybeRequestPushPermission();
        void qc.invalidateQueries({ queryKey: queryKeys.stories.list() });
        void qc.invalidateQueries({ queryKey: queryKeys.stories.featured() });
        router.replace(`/story/${storyId}`);
      }
    })();

    return () => {
      cancelled = true;
      cancelRef.current?.();
    };
  }, [ageGroup, theme]);

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" />
      <Text style={styles.title}>
        {phase === 'starting' && t('create.starting')}
        {phase === 'streaming' && t('create.streaming')}
        {phase === 'done' && t('create.done')}
        {phase === 'error' && (error ?? t('errors.ERROR'))}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: { flex: 1, backgroundColor: theme.colors.bg, alignItems: 'center', justifyContent: 'center', gap: 24 },
  title: { fontSize: 20, color: theme.colors.text, fontWeight: theme.scalars.titleWeight },
}));
