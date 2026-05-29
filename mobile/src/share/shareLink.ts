import * as Sharing from 'expo-sharing';

export async function shareLink(storyId: string, title: string): Promise<void> {
  const url = `https://kazkatales.com/story/${storyId}`;
  if (!(await Sharing.isAvailableAsync())) return;
  await Sharing.shareAsync(url, { dialogTitle: title });
}
