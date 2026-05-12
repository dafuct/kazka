import { captureRef } from 'react-native-view-shot';
import * as Sharing from 'expo-sharing';
import type { RefObject } from 'react';
import type { View } from 'react-native';

export async function shareImage(viewRef: RefObject<View | null>, dialogTitle: string): Promise<void> {
  if (!viewRef.current) return;
  const uri = await captureRef(viewRef.current, { format: 'png', quality: 0.9 });
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(uri, { dialogTitle, mimeType: 'image/png' });
  }
}
