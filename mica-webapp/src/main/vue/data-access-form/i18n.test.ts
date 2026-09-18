import { describe, it, expect, vi, afterEach } from 'vitest';
import { messages as libraryMessages } from '@obiba/quasar-ui-json-form';
import { createFormI18n, deepMerge, loadMicaTranslations } from './i18n';

describe('deepMerge', () => {
  it('merges nested objects, the override winning on scalars', () => {
    expect(deepMerge({ a: { x: 1, y: 2 }, b: 'base' }, { a: { y: 3, z: 4 }, b: 'over', c: true })).toEqual({
      a: { x: 1, y: 3, z: 4 },
      b: 'over',
      c: true,
    });
  });

  it('never lets a scalar replace a message namespace (Mica error: "Error" vs error.required)', () => {
    const base = { error: { required: 'Required' }, files: { upload: 'Upload' } };
    const merged = deepMerge(base, { error: 'Error', files: { upload: 'Send' }, upload: 'Upload' });
    expect(merged).toEqual({ error: { required: 'Required' }, files: { upload: 'Send' }, upload: 'Upload' });
  });

  it('lets an object replace a scalar and does not mutate its inputs', () => {
    const base = { k: 'v' };
    const merged = deepMerge(base, { k: { nested: true } });
    expect(merged).toEqual({ k: { nested: true } });
    expect(base).toEqual({ k: 'v' });
  });
});

describe('createFormI18n', () => {
  it('starts with the library messages of the language, english as fallback', () => {
    const i18n = createFormI18n('fr');
    expect(i18n.global.locale.value).toBe('fr');
    expect(i18n.global.t('error.required')).toBe((libraryMessages as any).fr.error.required);
    expect(createFormI18n('de').global.t('error.required')).toBe((libraryMessages as any).en.error.required);
  });
});

describe('loadMicaTranslations', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('fetches the Mica bundle and merges it over the library messages', async () => {
    const fetch = vi.fn(async () => ({ ok: true, json: async () => ({ error: 'Erreur', language: { fr: 'Français' }, files: { upload: 'Envoyer' } }) }));
    vi.stubGlobal('fetch', fetch);
    const i18n = createFormI18n('fr');
    await loadMicaTranslations(i18n, '/mica', 'fr');
    expect(fetch).toHaveBeenCalledWith('/mica/ws/config/i18n/fr.json', expect.objectContaining({ headers: { Accept: 'application/json' } }));
    expect(i18n.global.t('language.fr')).toBe('Français');
    expect(i18n.global.t('files.upload')).toBe('Envoyer');
    expect(i18n.global.t('error.required')).toBe((libraryMessages as any).fr.error.required);
  });

  it('rejects when the bundle cannot be loaded', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 500 })));
    await expect(loadMicaTranslations(createFormI18n('en'), '', 'en')).rejects.toThrow('500');
  });
});
