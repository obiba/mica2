import { api } from 'src/boot/api';
import type { PluginDto, PluginPackageDto, PluginPackagesDto } from 'src/models/MicaPlugins';
import { notifyError } from 'src/utils/notify';

/** the plugin types the server knows */
export const PLUGIN_TYPES = ['mica-search', 'mica-tables', 'mica-taxonomies'] as const;

/**
 * The server plugins: the installed ones (with the update site and whether a restart is required),
 * the updatable and available ones from the update site, and the actions on them. Installations and
 * uninstallations are only effective after the server is restarted.
 */
export function usePlugins() {
  const installed = ref<PluginPackagesDto>();
  /** the name of the plugin being acted on */
  const busy = ref<string>();

  async function load(): Promise<void> {
    try {
      installed.value = (await api.get<PluginPackagesDto>('/config/plugins')).data;
    } catch (error) {
      notifyError(error);
    }
  }

  async function loadPackages(kind: 'updates' | 'available'): Promise<PluginPackageDto[]> {
    try {
      return (await api.get<PluginPackagesDto>(`/config/plugins/_${kind}`)).data.packages || [];
    } catch (error) {
      notifyError(error);
      return [];
    }
  }

  async function act(name: string, request: () => Promise<unknown>): Promise<boolean> {
    busy.value = name;
    try {
      await request();
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      busy.value = undefined;
    }
  }

  function install(pkg: PluginPackageDto): Promise<boolean> {
    return act(pkg.name, () => api.post('/config/plugins', {}, { params: { name: pkg.name, version: pkg.version } }));
  }

  function uninstall(name: string): Promise<boolean> {
    return act(name, () => api.delete(`/config/plugin/${name}`));
  }

  function cancelUninstall(name: string): Promise<boolean> {
    return act(name, () => api.put(`/config/plugin/${name}`));
  }

  /** stops then starts the plugin service */
  function restart(name: string): Promise<boolean> {
    return act(name, async () => {
      await api.delete(`/config/plugin/${name}/service`);
      await api.put(`/config/plugin/${name}/service`);
    });
  }

  async function getSiteProperties(name: string): Promise<string> {
    return (await api.get<PluginDto>(`/config/plugin/${name}`)).data.siteProperties || '';
  }

  function saveSiteProperties(name: string, properties: string): Promise<boolean> {
    return act(name, () =>
      api.put(`/config/plugin/${name}/cfg`, properties, { headers: { 'Content-Type': 'text/plain' } }),
    );
  }

  return {
    installed,
    busy,
    load,
    loadPackages,
    install,
    uninstall,
    cancelUninstall,
    restart,
    getSiteProperties,
    saveSiteProperties,
  };
}
