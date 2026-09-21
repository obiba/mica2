import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import type { MicaConfigDto } from 'src/models/Mica';
import { INDICES, enabledIndices, useIndexing } from './useIndexing';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;

function index(key: string) {
  const found = INDICES.find((i) => i.key === key);
  if (!found) throw new Error(key);
  return found;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useIndexing', () => {
  it('rebuilds all the indices', async () => {
    mocked.put.mockResolvedValueOnce({ status: 204 });
    const { busy, rebuildAll } = useIndexing();
    const pending = rebuildAll();
    expect(busy.value).toBe('all');
    expect(await pending).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/config/_index');
    expect(busy.value).toBeUndefined();
  });

  it('rebuilds one index', async () => {
    mocked.put.mockResolvedValueOnce({ status: 204 });
    const { rebuild } = useIndexing();
    expect(await rebuild(index('studies'))).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/draft/studies/_index');
  });

  it('reports a failed rebuild', async () => {
    mocked.put.mockRejectedValueOnce(new Error('403'));
    const { busy, rebuild } = useIndexing();
    expect(await rebuild(index('taxonomies'))).toBe(false);
    expect(busy.value).toBeUndefined();
  });

  it('offers the indices of the enabled sections', () => {
    const all = {
      isNetworkEnabled: true,
      isCollectedDatasetEnabled: true,
      isHarmonizedDatasetEnabled: true,
      isProjectEnabled: true,
    } as MicaConfigDto;
    expect(enabledIndices(all).map((i) => i.key)).toEqual([
      'networks',
      'studies',
      'datasets',
      'collectedDatasets',
      'harmonizedDatasets',
      'persons',
      'files',
      'projects',
      'taxonomies',
    ]);
    const collectedOnly = { isCollectedDatasetEnabled: true } as MicaConfigDto;
    expect(enabledIndices(collectedOnly).map((i) => i.key)).toEqual([
      'studies',
      'collectedDatasets',
      'persons',
      'files',
      'taxonomies',
    ]);
  });
});
