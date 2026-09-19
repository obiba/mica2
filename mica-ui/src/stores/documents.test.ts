import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { api } from 'src/boot/api';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentsStore } from './documents';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}));

const mocked = api as unknown as Record<'get' | 'post' | 'put', ReturnType<typeof vi.fn>>;
const state = { revisionStatus: 'DRAFT', revisionsAhead: 0, requireIndexing: false };

beforeEach(() => {
  setActivePinia(createPinia());
  vi.clearAllMocks();
});

describe('documents store', () => {
  it('lists the documents from the list path of the type', async () => {
    mocked.get.mockResolvedValueOnce({ data: [{ id: 'a' }] });
    const store = useDocumentsStore();
    await store.fetchDocuments(documentTarget('individual-study', ''));
    expect(mocked.get).toHaveBeenCalledWith('/draft/study-states', {
      params: { type: 'individual-study', from: 0, limit: 1000, order: 'asc', sort: 'id' },
    });
    expect(store.listOf('individual-study')).toEqual([{ id: 'a' }]);
    expect(store.listOf('network')).toEqual([]);
  });

  it('unwraps the projects list', async () => {
    const request = { id: 'dar1', status: 'APPROVED', viewable: true };
    mocked.get.mockResolvedValueOnce({ data: { from: 0, limit: 1000, total: 1, projects: [{ id: 'p1', request }] } });
    const store = useDocumentsStore();
    const list = await store.fetchDocuments(documentTarget('project', ''));
    expect(mocked.get).toHaveBeenCalledWith('/draft/projects', {
      params: { from: 0, limit: 1000, order: 'asc', sort: 'id' },
    });
    expect(list).toEqual([{ id: 'p1', request }]);
    expect(store.listOf('project')).toEqual(list);
  });

  it('reads the state from the document when it carries it', async () => {
    mocked.get.mockResolvedValueOnce({ data: { id: 'net1', name: [], state } });
    const loaded = await useDocumentsStore().fetchDocument(documentTarget('network', 'net1'));
    expect(mocked.get).toHaveBeenCalledTimes(1);
    expect(loaded.document.id).toBe('net1');
    expect(loaded.state).toEqual(state);
  });

  it('fetches the state of a study apart', async () => {
    mocked.get.mockResolvedValueOnce({ data: { id: 's1', name: [] } });
    mocked.get.mockResolvedValueOnce({ data: { id: 's1', state } });
    const loaded = await useDocumentsStore().fetchDocument(documentTarget('individual-study', 's1'));
    expect(mocked.get).toHaveBeenNthCalledWith(1, '/draft/individual-study/s1');
    expect(mocked.get).toHaveBeenNthCalledWith(2, '/draft/study-state/s1');
    expect(loaded.state).toEqual(state);
  });

  it('creates and saves through the target paths', async () => {
    mocked.post.mockResolvedValueOnce({ headers: { location: 'http://x/ws/draft/collected-datasets/ds1' } });
    mocked.put.mockResolvedValueOnce({});
    const store = useDocumentsStore();
    const target = documentTarget('collected-dataset', 'ds1');
    const dto = store.newDocument('collected-dataset');
    expect(dto).toMatchObject({ entityType: 'Participant', name: [] });
    expect(await store.createDocument(target, dto)).toBe('ds1');
    expect(mocked.post).toHaveBeenCalledWith('/draft/collected-datasets', dto);
    await store.saveDocument(target, dto, 'why');
    expect(mocked.put).toHaveBeenCalledWith('/draft/collected-dataset/ds1', dto, { params: { comment: 'why' } });
  });
});
