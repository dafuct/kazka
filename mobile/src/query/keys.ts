// Centralised query key factory. Avoids stringly-typed keys spread across screens.

export const queryKeys = {
  stories: {
    all: ['stories'] as const,
    list: () => [...queryKeys.stories.all, 'list'] as const,
    byId: (id: string) => [...queryKeys.stories.all, 'byId', id] as const,
    featured: () => [...queryKeys.stories.all, 'featured'] as const,
  },
  auth: {
    me: ['auth', 'me'] as const,
  },
};
