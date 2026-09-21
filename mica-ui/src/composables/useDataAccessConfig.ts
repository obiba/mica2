import { api } from 'src/boot/api';
import type { DataAccessConfigDto } from 'src/models/Mica';
import { notifyError } from 'src/utils/notify';

/**
 * The data access configuration (`/config/data-access`): the enabled forms, the notifications, the
 * workflow... replaced as a whole on save.
 */
export function useDataAccessConfig() {
  const loading = ref(false);
  const saving = ref(false);
  const config = ref<DataAccessConfigDto>();

  async function load(): Promise<void> {
    loading.value = true;
    try {
      const response = await api.get<DataAccessConfigDto>('/config/data-access');
      config.value = response.data;
    } catch (error) {
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  /** saves the whole configuration, and reloads it */
  async function save(updated: DataAccessConfigDto): Promise<boolean> {
    saving.value = true;
    try {
      await api.put('/config/data-access', updated);
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      saving.value = false;
    }
  }

  return { loading, saving, config, load, save };
}
