import { QueryClient } from '@tanstack/react-query';
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import { queryStorage } from './storage';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,                 // 1 min — UI doesn't refetch on every focus
      gcTime: 24 * 60 * 60 * 1000,        // 24 h — keeps data for offline read
      retry: (failureCount, error) => {
        // Don't retry 4xx (the API client already handles 401 → refresh).
        const status = (error as { status?: number })?.status;
        if (status != null && status >= 400 && status < 500) return false;
        return failureCount < 2;
      },
    },
  },
});

export const queryPersister = createAsyncStoragePersister({
  storage: queryStorage,
  key: 'kazka.query.cache',
});
