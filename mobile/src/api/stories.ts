import { apiClient } from './client';
import type { ListArgs, Story, StoryCursorPage, UpdateStoryArgs } from './types';

export const storiesApi = {
  list: (args: ListArgs = {}): Promise<StoryCursorPage> => {
    const params = new URLSearchParams();
    params.set('limit', String(args.limit ?? 20));
    if (args.cursor) params.set('cursor', args.cursor);
    return apiClient.get(`/api/stories/cursor?${params.toString()}`);
  },

  byId: (id: string): Promise<Story> => apiClient.get(`/api/stories/${id}`),

  featured: async (): Promise<Story | null> => {
    const result = await apiClient.get<Story | undefined>('/api/stories/featured');
    return result ?? null;
  },

  update: (id: string, body: UpdateStoryArgs): Promise<Story> =>
    apiClient.put(`/api/stories/${id}`, body),

  remove: (id: string): Promise<void> => apiClient.delete(`/api/stories/${id}`),
};
