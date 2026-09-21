import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import { BUILDABLE_CACHE_IDS, CACHE_IDS, useCaches } from './useCaches';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useCaches', () => {
  it('clears all the caches', async () => {
    mocked.delete.mockResolvedValueOnce({ status: 200 });
    const { busy, clearAll } = useCaches();
    const pending = clearAll();
    expect(busy.value).toBe('all');
    expect(await pending).toBe(true);
    expect(mocked.delete).toHaveBeenCalledWith('/caches');
    expect(busy.value).toBeUndefined();
  });

  it('clears one cache', async () => {
    mocked.delete.mockResolvedValueOnce({ status: 200 });
    const { clear } = useCaches();
    expect(await clear('authorization')).toBe(true);
    expect(mocked.delete).toHaveBeenCalledWith('/cache/authorization');
  });

  it('builds the dataset variables cache', async () => {
    mocked.put.mockResolvedValueOnce({ status: 200 });
    const { build } = useCaches();
    expect(await build('datasetVariables')).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/cache/datasetVariables');
  });

  it('reports a failed action and releases the busy state', async () => {
    mocked.delete.mockRejectedValueOnce(new Error('500'));
    const { busy, clear } = useCaches();
    expect(await clear('micaConfig')).toBe(false);
    expect(notifyError).toHaveBeenCalled();
    expect(busy.value).toBeUndefined();
  });

  it('declares the caches the server knows', () => {
    expect(CACHE_IDS).toEqual([
      'micaConfig',
      'variableTaxonomies',
      'aggregationsMetadata',
      'datasetVariables',
      'authorization',
    ]);
    expect(BUILDABLE_CACHE_IDS).toEqual(['datasetVariables']);
  });
});
