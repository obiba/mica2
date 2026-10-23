import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import type { PluginPackageDto } from 'src/models/MicaPlugins';
import { usePlugins } from './usePlugins';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;

const installed = { site: 'https://plugins.obiba.org', restart: false, packages: [] };

beforeEach(() => {
  vi.clearAllMocks();
  mocked.get.mockResolvedValue({ data: installed });
});

describe('usePlugins', () => {
  it('loads the installed plugins', async () => {
    const { installed: value, load } = usePlugins();
    await load();
    expect(mocked.get).toHaveBeenCalledWith('/config/plugins');
    expect(value.value).toEqual(installed);
  });

  it('loads the updatable and available packages', async () => {
    mocked.get.mockResolvedValueOnce({ data: { ...installed, packages: [{ name: 'p' }] } });
    const { loadPackages } = usePlugins();
    expect(await loadPackages('updates')).toEqual([{ name: 'p' }]);
    expect(mocked.get).toHaveBeenCalledWith('/config/plugins/_updates');
    expect(await loadPackages('available')).toEqual([]);
    expect(mocked.get).toHaveBeenCalledWith('/config/plugins/_available');
  });

  it('installs a package version, then reloads', async () => {
    mocked.post.mockResolvedValueOnce({});
    const { install } = usePlugins();
    expect(await install({ name: 'p', version: '1.0.0' } as PluginPackageDto)).toBe(true);
    expect(mocked.post).toHaveBeenCalledWith('/config/plugins', {}, { params: { name: 'p', version: '1.0.0' } });
    expect(mocked.get).toHaveBeenCalledWith('/config/plugins');
  });

  it('uninstalls and cancels the uninstallation', async () => {
    mocked.delete.mockResolvedValue({});
    mocked.put.mockResolvedValue({});
    const { uninstall, cancelUninstall } = usePlugins();
    await uninstall('p');
    expect(mocked.delete).toHaveBeenCalledWith('/config/plugin/p');
    await cancelUninstall('p');
    expect(mocked.put).toHaveBeenCalledWith('/config/plugin/p');
  });

  it('restarts the service by stopping then starting it', async () => {
    const calls: string[] = [];
    mocked.delete.mockImplementation(async () => calls.push('stop'));
    mocked.put.mockImplementation(async () => calls.push('start'));
    const { busy, restart } = usePlugins();
    const pending = restart('p');
    expect(busy.value).toBe('p');
    expect(await pending).toBe(true);
    expect(calls).toEqual(['stop', 'start']);
    expect(mocked.delete).toHaveBeenCalledWith('/config/plugin/p/service');
    expect(mocked.put).toHaveBeenCalledWith('/config/plugin/p/service');
    expect(busy.value).toBeUndefined();
  });

  it('reads and saves the site properties as plain text', async () => {
    mocked.get.mockResolvedValueOnce({ data: { siteProperties: 'a=1' } });
    mocked.put.mockResolvedValueOnce({});
    const { getSiteProperties, saveSiteProperties } = usePlugins();
    expect(await getSiteProperties('p')).toBe('a=1');
    expect(await saveSiteProperties('p', 'a=2')).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/config/plugin/p/cfg', 'a=2', {
      headers: { 'Content-Type': 'text/plain' },
    });
  });

  it('reports a failed action and releases the busy state', async () => {
    mocked.delete.mockRejectedValueOnce(new Error('500'));
    const { busy, uninstall } = usePlugins();
    expect(await uninstall('p')).toBe(false);
    expect(notifyError).toHaveBeenCalled();
    expect(busy.value).toBeUndefined();
  });
});
