import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { fromDefinition } from '@obiba/quasar-ui-json-form/builder';
import type { FormDefinition } from '@obiba/quasar-ui-json-form/builder';
import { api } from 'src/boot/api';
import { EntityFormDto_Type } from 'src/models/Mica';
import { prepareForm, useEntityConfigForm, type EntityConfigTarget } from './useEntityConfigForm';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'put', ReturnType<typeof vi.fn>>;
const target: EntityConfigTarget = { name: 'network', type: EntityFormDto_Type.Network };

const asfForm = {
  type: 'Network',
  schema: JSON.stringify({
    type: 'object',
    properties: { website: { title: 't(website)', type: 'string' } },
    required: [],
  }),
  definition: JSON.stringify(['website', { type: 'help', helpvalue: '<p>t(website) info</p>' }]),
};

const bundles: Record<string, unknown> = {
  en: { website: 'Website', other: 'Other', network: { name: 'Name' } },
  fr: { website: 'Site web', other: 'Autre' },
};

function mockServer() {
  mocked.get.mockImplementation((url: string) => {
    if (url === '/config/network/form-custom') return Promise.resolve({ data: asfForm });
    const match = /^\/config\/i18n\/(\w+)\.json$/.exec(url);
    if (match) return Promise.resolve({ data: bundles[match[1] as string] });
    return Promise.reject(new Error(`unexpected ${url}`));
  });
  mocked.put.mockResolvedValue({ status: 200 });
}

beforeEach(() => {
  setActivePinia(createPinia());
  vi.clearAllMocks();
});

describe('useEntityConfigForm', () => {
  it('loads the custom form as a builder definition with the bundle translations', async () => {
    mockServer();
    const { form, load, dirty } = useEntityConfigForm(target, () => ['en', 'fr']);
    await load();
    expect(mocked.get).toHaveBeenCalledWith('/config/network/form-custom');
    expect(mocked.get).toHaveBeenCalledWith('/config/i18n/en.json');
    expect(mocked.get).toHaveBeenCalledWith('/config/i18n/fr.json');
    const loaded = form.value as FormDefinition;
    expect((loaded.schema.properties as Record<string, { title: string }>).website?.title).toBe('website');
    const elements = (loaded.uischema as { elements: Record<string, unknown>[] }).elements;
    expect(elements[0]).toEqual({ type: 'Control', scope: '#/properties/website' });
    // a mixed text keeps its token
    expect(elements[1]?.text).toBe('<p>t(website) info</p>');
    // only the keys the form uses
    expect(loaded.translations).toEqual({ en: { website: 'Website' }, fr: { website: 'Site web' } });
    expect(dirty.value).toBe(false);
  });

  it('saves the changed translations then the form as a JSON Forms pair, and reloads', async () => {
    mockServer();
    const { form, load, save } = useEntityConfigForm(target, () => ['en', 'fr']);
    await load();
    const loaded = form.value as FormDefinition;
    const edited: FormDefinition = {
      schema: {
        type: 'object',
        properties: {
          website: { title: 'website', type: 'string' },
          phone: { title: 'phone.title', type: 'string' },
        },
        required: [],
      },
      uischema: {
        type: 'VerticalLayout',
        elements: [
          ...(loaded.uischema as { elements: unknown[] }).elements,
          { type: 'Control', scope: '#/properties/phone', hint: 'phone.hint' },
        ],
      },
      translations: {
        en: { website: 'Web site', 'phone.title': 'Phone', 'phone.hint': 'Digits only' },
        fr: { website: 'Site web', 'phone.title': '', 'phone.hint': 'Chiffres seulement' },
      },
    };
    mocked.get.mockClear();
    expect(await save(fromDefinition(edited))).toBe(true);

    expect(mocked.put).toHaveBeenNthCalledWith(
      1,
      '/config/i18n/custom/import',
      {
        en: { website: 'Web site', 'network-form': { phone: { title: 'Phone', hint: 'Digits only' } } },
        fr: { 'network-form': { phone: { hint: 'Chiffres seulement' } } },
      },
      { params: { merge: true } },
    );
    const [path, dto] = mocked.put.mock.calls[1] as [string, { type: string; schema: string; definition: string }];
    expect(path).toBe('/config/network/form-custom');
    expect(dto.type).toBe('Network');
    expect(JSON.parse(dto.schema).properties).toEqual({
      website: { title: 't(website)', type: 'string' },
      phone: { title: 't(network-form.phone.title)', type: 'string' },
    });
    expect(JSON.parse(dto.definition)).toEqual({
      type: 'VerticalLayout',
      elements: [
        { type: 'Control', scope: '#/properties/website' },
        { type: 'Label', text: '<p>t(website) info</p>' },
        { type: 'Control', scope: '#/properties/phone', hint: 't(network-form.phone.hint)' },
      ],
    });
    // reloaded
    expect(mocked.get).toHaveBeenCalledWith('/config/network/form-custom');
  });

  it('reports a failed save', async () => {
    mockServer();
    const { form, load, save } = useEntityConfigForm(target, () => ['en']);
    await load();
    mocked.put.mockRejectedValueOnce(new Error('400'));
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(false);
  });
});

describe('prepareForm', () => {
  it('keeps a key already prefixed and skips the unchanged translations', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { phone: { title: 'network-form.phone.title', type: 'string' } } },
      uischema: { type: 'VerticalLayout', elements: [{ type: 'Control', scope: '#/properties/phone' }] },
      translations: { en: { 'network-form.phone.title': 'Phone' } },
    });
    const prepared = prepareForm(model, target, new Set(['network-form.phone.title']), {
      en: { 'network-form.phone.title': 'Phone' },
    });
    expect((prepared.schema.properties as Record<string, { title: string }>).phone?.title).toBe(
      't(network-form.phone.title)',
    );
    expect(prepared.translations).toEqual({});
  });

  it('leaves a literal as it is', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { phone: { title: 'Phone', type: 'string' } } },
      uischema: { type: 'VerticalLayout', elements: [{ type: 'Control', scope: '#/properties/phone' }] },
    });
    const prepared = prepareForm(model, target, new Set(), {});
    expect((prepared.schema.properties as Record<string, { title: string }>).phone?.title).toBe('Phone');
  });
});
