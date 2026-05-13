import * as Notifications from 'expo-notifications';
import { router } from 'expo-router';

// In-foreground behavior: show the banner + play sound.
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

/**
 * Subscribe to notification taps (background → user opens). Returns a cleanup
 * function that removes the listener. Call from the root layout's useEffect.
 */
export function subscribeToTaps(): () => void {
  const sub = Notifications.addNotificationResponseReceivedListener((response) => {
    const data = response.notification.request.content.data as { storyId?: string; type?: string };
    if (data?.type === 'story_ready' && typeof data.storyId === 'string') {
      router.push(`/story/${data.storyId}`);
    }
  });
  return () => sub.remove();
}
