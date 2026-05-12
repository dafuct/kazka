import { useQuery, useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from './keys';
import { storiesApi } from '@/src/api/stories';
import type { Story } from '@/src/api/types';

export function useFeatured() {
  return useQuery({
    queryKey: queryKeys.stories.featured(),
    queryFn: () => storiesApi.featured(),
  });
}

export function useStory(id: string) {
  return useQuery({
    queryKey: queryKeys.stories.byId(id),
    queryFn: () => storiesApi.byId(id),
  });
}

export function useStoriesInfinite(limit: number = 20) {
  return useInfiniteQuery({
    queryKey: queryKeys.stories.list(),
    queryFn: ({ pageParam }) => storiesApi.list({ cursor: pageParam, limit }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
  });
}

export function useUpdateStory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) =>
      storiesApi.update(id, { title }),
    onSuccess: (story: Story) => {
      qc.setQueryData(queryKeys.stories.byId(story.id), story);
      void qc.invalidateQueries({ queryKey: queryKeys.stories.list() });
      void qc.invalidateQueries({ queryKey: queryKeys.stories.featured() });
    },
  });
}

export function useDeleteStory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => storiesApi.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.stories.list() });
      void qc.invalidateQueries({ queryKey: queryKeys.stories.featured() });
    },
  });
}
