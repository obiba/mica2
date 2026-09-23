import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { flatten, unflatten, useTranslations } from './useTranslations';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;

const defaults: Record<string, unknown> = {
  en: { study: { title: 'Study', label: 'Studies' }, search: 'Search', userProfile: { name: 'Name' } },
  fr: { study: { title: 'Étude', label: 'Études' }, search: 'Recherche', userProfile: { name: 'Nom' } },
};
const custom: Record<string, unknown> = {
  en: { study: { title: 'Cohort' }, extra: { hello: 'Hello' } },
  fr: {},
};

function mockLoad() {
  mocked.get.mockImplementation((url: string) => {
    const [, isCustom, locale] = /\/config\/i18n\/(custom\/)?(\w+)\.json/.exec(url)!;
    return Promise.resolve({ data: (isCustom ? custom : defaults)[locale!] });
  });
}

async function loaded() {
  mockLoad();
  const translations = useTranslations();
  expect(await translations.load(['en', 'fr'])).toBe(true);
  return translations;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('flatten / unflatten', () => {
  it('round trips a tree of translations', () => {
    const tree = { a: { b: 'B', c: { d: 'D' } }, e: 'E' };
    expect(flatten(tree)).toEqual({ 'a.b': 'B', 'a.c.d': 'D', e: 'E' });
    expect(unflatten(flatten(tree))).toEqual(tree);
  });
});

describe('useTranslations', () => {
  it('merges the default and custom keys, without the user profile ones', async () => {
    const { keys, value, isCustomized, hasDefault, customizedCounts, dirty } = await loaded();
    expect(keys.value).toEqual(['extra.hello', 'search', 'study.label', 'study.title']);
    expect(value('study.title', 'en')).toBe('Cohort');
    expect(value('study.title', 'fr')).toBe('Étude');
    expect(value('extra.hello', 'fr')).toBe('');
    expect(isCustomized('study.title', 'en')).toBe(true);
    expect(isCustomized('study.title', 'fr')).toBe(false);
    expect(hasDefault('extra.hello')).toBe(false);
    expect(customizedCounts.value).toEqual({ en: 2, fr: 0 });
    expect(dirty.value).toBe(false);
  });

  it('filters by key or value in any language', async () => {
    const { filterKeys } = await loaded();
    expect(filterKeys('ÉTUDE')).toEqual(['study.label', 'study.title']);
    expect(filterKeys('search')).toEqual(['search']);
    expect(filterKeys('')).toHaveLength(4);
    expect(filterKeys('', true)).toEqual(['extra.hello', 'study.title']);
  });

  it('forgets a value set back to the default, and resets', async () => {
    const { setValue, reset, isCustomized, dirty } = await loaded();
    setValue('search', 'fr', 'Chercher');
    expect(isCustomized('search', 'fr')).toBe(true);
    expect(dirty.value).toBe(true);
    setValue('search', 'fr', 'Recherche');
    expect(isCustomized('search', 'fr')).toBe(false);
    expect(dirty.value).toBe(false);
    reset('study.title', 'en');
    expect(isCustomized('study.title', 'en')).toBe(false);
  });

  it('adds and removes custom keys', async () => {
    const { addKey, removeKey, keys } = await loaded();
    expect(addKey('new.key')).toBe(true);
    expect(addKey('search')).toBe(false);
    expect(addKey('bad..key')).toBe(false);
    expect(addKey('userProfile.x')).toBe(false);
    expect(keys.value).toContain('new.key');
    removeKey('new.key');
    expect(keys.value).not.toContain('new.key');
  });

  it('saves the customized values of each language as a tree', async () => {
    const { setValue, save, dirty } = await loaded();
    setValue('study.label', 'fr', 'Cohortes');
    mocked.put.mockResolvedValue({ status: 200 });
    expect(await save()).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith(
      '/config/i18n/custom/en.json',
      { study: { title: 'Cohort' }, extra: { hello: 'Hello' } },
      { params: { merge: false } },
    );
    expect(mocked.put).toHaveBeenCalledWith(
      '/config/i18n/custom/fr.json',
      { study: { label: 'Cohortes' } },
      { params: { merge: false } },
    );
    expect(dirty.value).toBe(false);
  });

  it('imports with each language in a replace, as is in a merge', async () => {
    const { importAll } = await loaded();
    mocked.put.mockResolvedValue({ status: 200 });
    expect(await importAll('{"en": {"a": "A"}}', false)).toBe(true);
    expect(mocked.put).toHaveBeenLastCalledWith(
      '/config/i18n/custom/import',
      { en: { a: 'A' }, fr: {} },
      { params: { merge: false } },
    );
    expect(await importAll('{"en": {"a": "A"}}', true)).toBe(true);
    expect(mocked.put).toHaveBeenLastCalledWith(
      '/config/i18n/custom/import',
      { en: { a: 'A' } },
      { params: { merge: true } },
    );
    expect(await importAll('[1]', true)).toBe(false);
    expect(await importAll('not json', true)).toBe(false);
  });
});
