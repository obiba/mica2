import { api } from 'src/boot/api';
import type { MicaConfigDto } from 'src/models/Mica';
import { notifyError } from 'src/utils/notify';

export type IndexKey =
  | 'networks'
  | 'studies'
  | 'datasets'
  | 'collectedDatasets'
  | 'harmonizedDatasets'
  | 'persons'
  | 'files'
  | 'projects'
  | 'taxonomies';

export interface IndexInfo {
  key: IndexKey;
  /** REST path of the rebuild request (`PUT`) */
  path: string;
  icon: string;
  /** whether the index is offered, by the Mica configuration */
  isEnabled: (configuration: MicaConfigDto) => boolean;
  /** i18n key of the reminder shown after the rebuild: the studies annotations depend on the datasets */
  warning?: string;
}

/** the rebuild request of every index (`PUT /config/_index`) */
export const ALL_INDICES_PATH = '/config/_index';

export const ALL_INDICES_WARNING = 'config.indexing.annotations_warning';

export const INDICES: readonly IndexInfo[] = [
  { key: 'networks', path: '/draft/networks/_index', icon: 'hub', isEnabled: (c) => c.isNetworkEnabled === true },
  { key: 'studies', path: '/draft/studies/_index', icon: 'science', isEnabled: () => true },
  {
    key: 'datasets',
    path: '/draft/datasets/_index',
    icon: 'dataset',
    isEnabled: (c) => c.isCollectedDatasetEnabled === true && c.isHarmonizedDatasetEnabled === true,
    warning: 'config.indexing.annotations_warning',
  },
  {
    key: 'collectedDatasets',
    path: '/draft/collected-datasets/_index',
    icon: 'table_chart',
    isEnabled: (c) => c.isCollectedDatasetEnabled === true,
    warning: 'config.indexing.annotations_warning_study',
  },
  {
    key: 'harmonizedDatasets',
    path: '/draft/harmonized-datasets/_index',
    icon: 'join_inner',
    isEnabled: (c) => c.isHarmonizedDatasetEnabled === true,
    warning: 'config.indexing.annotations_warning_initiative',
  },
  { key: 'persons', path: '/draft/persons/_index', icon: 'people', isEnabled: () => true },
  { key: 'files', path: '/draft/files/_index', icon: 'folder', isEnabled: () => true },
  { key: 'projects', path: '/draft/projects/_index', icon: 'work', isEnabled: (c) => c.isProjectEnabled === true },
  { key: 'taxonomies', path: '/taxonomies/_index', icon: 'account_tree', isEnabled: () => true },
];

/** the indices offered by the configuration */
export function enabledIndices(configuration: MicaConfigDto): IndexInfo[] {
  return INDICES.filter((index) => index.isEnabled(configuration));
}

/**
 * The search indices: each one can be rebuilt, or all of them at once. The rebuild runs in the
 * background on the server, the request returns as soon as it is scheduled.
 */
export function useIndexing() {
  /** the key of the index being rebuilt, `all` when every index is */
  const busy = ref<IndexKey | 'all'>();

  async function act(key: IndexKey | 'all', path: string): Promise<boolean> {
    busy.value = key;
    try {
      await api.put(path);
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      busy.value = undefined;
    }
  }

  function rebuildAll(): Promise<boolean> {
    return act('all', ALL_INDICES_PATH);
  }

  function rebuild(index: IndexInfo): Promise<boolean> {
    return act(index.key, index.path);
  }

  return { busy, rebuildAll, rebuild };
}
