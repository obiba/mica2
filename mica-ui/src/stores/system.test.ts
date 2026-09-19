import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { api } from 'src/boot/api';
import type { MicaConfigDto } from 'src/models/Mica';
import { useSystemStore } from './system';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), put: vi.fn() },
}));

const mocked = api as unknown as Record<'get' | 'put', ReturnType<typeof vi.fn>>;

beforeEach(() => {
  setActivePinia(createPinia());
  vi.clearAllMocks();
});

describe('system store', () => {
  it('saves the configuration then reloads it and its public view', async () => {
    const config = { name: 'Mica', languages: ['en', 'fr'] } as MicaConfigDto;
    mocked.put.mockResolvedValueOnce({ status: 204 });
    mocked.get.mockImplementation((path: string) =>
      Promise.resolve({ status: 200, data: path === '/config' ? config : { name: 'Mica', languages: ['en', 'fr'] } }),
    );
    const store = useSystemStore();
    await store.save(config);
    expect(mocked.put).toHaveBeenCalledWith('/config', config);
    expect(mocked.get).toHaveBeenCalledWith('/config');
    expect(mocked.get).toHaveBeenCalledWith('/config/_public');
    expect(store.configuration).toEqual(config);
    expect(store.languages).toEqual(['en', 'fr']);
  });

  it('does not reload the configuration when the save fails', async () => {
    mocked.put.mockRejectedValueOnce(new Error('403'));
    const store = useSystemStore();
    await expect(store.save({ name: 'Mica' } as MicaConfigDto)).rejects.toThrow('403');
    expect(mocked.get).not.toHaveBeenCalled();
  });

  it('caches the languages by locale', async () => {
    mocked.get.mockResolvedValueOnce({ status: 200, data: { en: 'English', fr: 'French' } });
    const store = useSystemStore();
    const first = await store.loadLanguages('en');
    const second = await store.loadLanguages('en');
    expect(mocked.get).toHaveBeenCalledTimes(1);
    expect(mocked.get).toHaveBeenCalledWith('/config/languages', { params: { locale: 'en' } });
    expect(second).toEqual(first);
    expect(store.availableLanguages.en).toEqual({ en: 'English', fr: 'French' });
  });

  it('loads the taxonomies summary', async () => {
    const summaries = [{ name: 'Mlstr_area', title: [{ locale: 'en', text: 'Areas' }] }];
    mocked.get.mockResolvedValueOnce({ status: 200, data: { summaries } });
    const store = useSystemStore();
    await store.loadTaxonomiesSummary();
    expect(mocked.get).toHaveBeenCalledWith('/taxonomies/_summary', { params: { vocabularies: false } });
    expect(store.taxonomiesSummary).toEqual(summaries);
  });
});
