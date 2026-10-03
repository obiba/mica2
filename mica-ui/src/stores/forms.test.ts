import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn() },
  toServerUrl: (path: string) => path,
}));

import { api } from 'src/boot/api';
import { useFormsStore } from './forms';

const formResponse = { data: { schema: '{"type":"object"}', definition: '["*"]' } };
const bundleResponse = { data: { study: { name: 'Name' } } };

beforeEach(() => {
  setActivePinia(createPinia());
  vi.mocked(api.get).mockReset();
});

describe('forms store', () => {
  it('shares one request between concurrent getForm() calls', async () => {
    vi.mocked(api.get).mockResolvedValue(formResponse);
    const store = useFormsStore();
    const [a, b] = await Promise.all([store.getForm('/config/study/form', 'en'), store.getForm('/config/study/form', 'en')]);
    expect(api.get).toHaveBeenCalledTimes(1);
    expect(a).toBe(b);
    await store.getForm('/config/study/form', 'en');
    expect(api.get).toHaveBeenCalledTimes(1);
  });

  it('requests a form per locale', async () => {
    vi.mocked(api.get).mockResolvedValue(formResponse);
    const store = useFormsStore();
    await Promise.all([store.getForm('/config/study/form', 'en'), store.getForm('/config/study/form', 'fr')]);
    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it('requests a form again after a failure', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(new Error('down')).mockResolvedValue(formResponse);
    const store = useFormsStore();
    await expect(store.getForm('/config/study/form', 'en')).rejects.toThrow('down');
    await expect(store.getForm('/config/study/form', 'en')).resolves.toEqual({ schema: { type: 'object' }, definition: ['*'] });
    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it('shares one request between concurrent getBundle() calls', async () => {
    vi.mocked(api.get).mockResolvedValue(bundleResponse);
    const store = useFormsStore();
    const [a, b] = await Promise.all([store.getBundle('en'), store.getBundle('en')]);
    expect(api.get).toHaveBeenCalledTimes(1);
    expect(a).toBe(b);
    expect(a['study.name']).toBe('Name');
  });

  it('requests a bundle per language', async () => {
    vi.mocked(api.get).mockResolvedValue(bundleResponse);
    const store = useFormsStore();
    await Promise.all([store.getBundle('en'), store.getBundle('fr')]);
    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it('resolves an empty bundle on failure, and requests it again next time', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(new Error('down')).mockResolvedValue(bundleResponse);
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    const store = useFormsStore();
    await expect(store.getBundle('en')).resolves.toEqual({});
    await expect(store.getBundle('en')).resolves.toEqual({ 'study.name': 'Name' });
    expect(api.get).toHaveBeenCalledTimes(2);
  });
});
