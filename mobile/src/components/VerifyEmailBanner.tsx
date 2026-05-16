import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { StyleSheet } from 'react-native-unistyles';
import { useAuthStore } from '@/src/stores/auth.store';
import { authApi } from '@/src/api/auth';

export function VerifyEmailBanner() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  if (!user || user.emailVerified) return null;

  return (
    <View style={styles.container}>
      <Text style={styles.text}>
        {sent ? t('verifyBanner.sent') : t('verifyBanner.message')}
      </Text>
      {!sent && (
        <Pressable
          accessibilityRole="button"
          disabled={sending}
          onPress={async () => {
            setSending(true);
            try {
              await authApi.resendVerification();
              setSent(true);
            } catch {
              // Keep banner visible; user can retry.
            }
            setSending(false);
          }}
        >
          <Text style={styles.action}>{t('verifyBanner.resend')}</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create((theme) => ({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 12,
    backgroundColor: theme.colors.surface2 ?? '#FFF3E0',
  },
  text: { flex: 1, color: theme.colors.text, fontSize: theme.scalars.bodySize },
  action: {
    color: theme.colors.accent ?? '#1E88E5',
    fontWeight: '600',
    paddingLeft: 12,
  },
}));
