import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';

/** the server caches an administrator can clear (`DELETE /cache/{id}`) */
export const CACHE_IDS = [
  'micaConfig',
  'variableTaxonomies',
  'aggregationsMetadata',
  'datasetVariables',
  'authorization',
] as const;

export type CacheId = (typeof CACHE_IDS)[number];

/** the caches that can also be built explicitly (`PUT /cache/{id}`) */
export const BUILDABLE_CACHE_IDS: readonly CacheId[] = ['datasetVariables'];

/**
 * The server caches: cleared one by one or all at once, the dataset variables statistics cache
 * can also be rebuilt from the Opal servers.
 */
export function useCaches() {
  /** the id of the cache being acted on, `all` when every cache is cleared */
  const busy = ref<CacheId | 'all'>();

  async function act(id: CacheId | 'all', request: () => Promise<unknown>): Promise<boolean> {
    busy.value = id;
    try {
      await request();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      busy.value = undefined;
    }
  }

  function clearAll(): Promise<boolean> {
    return act('all', () => api.delete('/caches'));
  }

  function clear(id: CacheId): Promise<boolean> {
    return act(id, () => api.delete(`/cache/${id}`));
  }

  function build(id: CacheId): Promise<boolean> {
    return act(id, () => api.put(`/cache/${id}`));
  }

  return { busy, clearAll, clear, build };
}
