import type { Story, StoryCursorPage } from '@kazka/shared';

export type { Story, StoryCursorPage };

export interface ListArgs {
  cursor?: string | null;
  limit?: number;
}

export interface UpdateStoryArgs {
  title: string;
  content: string;
}
