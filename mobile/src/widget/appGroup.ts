import AppGroup from '../../modules/AppGroup';

export interface WidgetPayload {
  title: string;
  snippet: string;
  storyId: string | null;
}

/** Writes today.json to the App Group container and triggers a WidgetKit reload. */
export async function writeTodayJSON(payload: WidgetPayload): Promise<void> {
  try {
    await AppGroup.writeJSON('today.json', payload as unknown as Record<string, unknown>);
    await AppGroup.reloadAllTimelines();
  } catch (e) {
    // Best-effort; failing here just leaves the widget showing its placeholder.
    if (__DEV__) console.warn('writeTodayJSON failed:', e);
  }
}
