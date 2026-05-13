import { useOfflineStore, type QueuedOp } from './offline.store';

describe('useOfflineStore', () => {
  beforeEach(() => {
    useOfflineStore.setState({ queue: [] });
  });

  it('starts empty', () => {
    expect(useOfflineStore.getState().queue).toEqual([]);
  });

  it('enqueues a rename op', () => {
    useOfflineStore.getState().enqueue({
      kind: 'rename',
      id: 'story-1',
      title: 'New Title',
      content: 'existing content',
    });
    expect(useOfflineStore.getState().queue).toHaveLength(1);
    expect(useOfflineStore.getState().queue[0]?.kind).toBe('rename');
  });

  it('removes an op by index', () => {
    useOfflineStore.getState().enqueue({
      kind: 'delete', id: 'story-1',
    } as QueuedOp);
    useOfflineStore.getState().enqueue({
      kind: 'delete', id: 'story-2',
    } as QueuedOp);
    useOfflineStore.getState().drainOne();
    expect(useOfflineStore.getState().queue).toHaveLength(1);
    expect(useOfflineStore.getState().queue[0]?.id).toBe('story-2');
  });

  it('clears the queue', () => {
    useOfflineStore.getState().enqueue({ kind: 'delete', id: 'x' } as QueuedOp);
    useOfflineStore.getState().clear();
    expect(useOfflineStore.getState().queue).toEqual([]);
  });
});
