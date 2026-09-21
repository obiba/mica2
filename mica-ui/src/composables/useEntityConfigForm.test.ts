import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { fromDefinition } from '@obiba/quasar-ui-json-form/builder';
import type { FormDefinition } from '@obiba/quasar-ui-json-form/builder';
import { api } from 'src/boot/api';
import { EntityFormDto_Type } from 'src/models/Mica';
import { parseTranslations, prepareForm, useEntityConfigForm, type EntityConfigTarget } from './useEntityConfigForm';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'put', ReturnType<typeof vi.fn>>;
const target: EntityConfigTarget = { name: 'network', type: EntityFormDto_Type.Network };

/** a legacy form: angular-schema-form, keys of the Mica bundle, no translations of its own */
const asfForm = {
  type: 'Network',
  schema: JSON.stringify({
    type: 'object',
    properties: { website: { title: 't(website)', type: 'string' } },
    required: [],
  }),
  definition: JSON.stringify(['website', { type: 'help', helpvalue: '<p>t(website) info</p>' }]),
};

/** a form saved by the builder, with its texts */
const jsonForm = {
  type: 'Network',
  schema: JSON.stringify({
    type: 'object',
    properties: {
      website: { title: 't(website)', type: 'string' },
      phone: { title: 't(phone.title)', type: 'string' },
    },
  }),
  definition: JSON.stringify({
    type: 'VerticalLayout',
    elements: [
      { type: 'Control', scope: '#/properties/website' },
      { type: 'Control', scope: '#/properties/phone', hint: 't(phone.hint)' },
    ],
  }),
  translations: JSON.stringify({
    en: { phone: { title: 'Phone', hint: 'Digits only' } },
    fr: { 'phone.title': 'Téléphone' },
  }),
};

function mockServer(dto: object, name = 'network') {
  mocked.get.mockImplementation((url: string) =>
    url === `/config/${name}/form-custom`
      ? Promise.resolve({ data: dto })
      : Promise.reject(new Error(`unexpected ${url}`)),
  );
  mocked.put.mockResolvedValue({ status: 200 });
}

beforeEach(() => {
  setActivePinia(createPinia());
  vi.clearAllMocks();
});

describe('parseTranslations', () => {
  it('reads the texts by language, and nothing else', () => {
    expect(parseTranslations(jsonForm.translations)).toEqual({
      en: { phone: { title: 'Phone', hint: 'Digits only' } },
      fr: { 'phone.title': 'Téléphone' },
    });
    expect(parseTranslations(undefined)).toEqual({});
    expect(parseTranslations('not json')).toEqual({});
    expect(parseTranslations('[]')).toEqual({});
  });
});

describe('useEntityConfigForm', () => {
  it('loads a legacy form, its bundle tokens kept as literals', async () => {
    mockServer(asfForm);
    const { form, load, dirty } = useEntityConfigForm(target);
    await load();
    expect(mocked.get).toHaveBeenCalledTimes(1);
    const loaded = form.value as FormDefinition;
    expect((loaded.schema.properties as Record<string, { title: string }>).website?.title).toBe('t(website)');
    const elements = (loaded.uischema as { elements: Record<string, unknown>[] }).elements;
    expect(elements[0]).toEqual({ type: 'Control', scope: '#/properties/website' });
    expect(elements[1]?.text).toBe('<p>t(website) info</p>');
    expect(loaded.translations).toEqual({});
    expect(dirty.value).toBe(false);
  });

  it('loads a form saved by the builder with its keys and texts, flat', async () => {
    mockServer(jsonForm);
    const { form, load } = useEntityConfigForm(target);
    await load();
    const loaded = form.value as FormDefinition;
    const properties = loaded.schema.properties as Record<string, { title: string }>;
    expect(properties.website?.title).toBe('t(website)');
    expect(properties.phone?.title).toBe('phone.title');
    const elements = (loaded.uischema as { elements: Record<string, unknown>[] }).elements;
    expect(elements[1]?.hint).toBe('phone.hint');
    expect(loaded.translations).toEqual({
      en: { 'phone.title': 'Phone', 'phone.hint': 'Digits only' },
      fr: { 'phone.title': 'Téléphone' },
    });
  });

  it('saves the form as a JSON Forms pair with its texts, and reloads', async () => {
    mockServer(asfForm);
    const { form, load, save } = useEntityConfigForm(target);
    await load();
    const loaded = form.value as FormDefinition;
    const edited: FormDefinition = {
      schema: {
        type: 'object',
        properties: {
          website: { title: 't(website)', type: 'string' },
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
        en: { 'phone.title': 'Phone', 'phone.hint': 'Digits only', orphan: 'Unused' },
        fr: { 'phone.title': '', 'phone.hint': 'Chiffres seulement' },
      },
    };
    mocked.get.mockClear();
    expect(await save(fromDefinition(edited))).toBe(true);

    expect(mocked.put).toHaveBeenCalledTimes(1);
    const [path, dto] = mocked.put.mock.calls[0] as [
      string,
      { type: string; schema: string; definition: string; translations?: string },
    ];
    expect(path).toBe('/config/network/form-custom');
    expect(dto.type).toBe('Network');
    expect(JSON.parse(dto.schema).properties).toEqual({
      website: { title: 't(website)', type: 'string' },
      phone: { title: 't(phone.title)', type: 'string' },
    });
    expect(JSON.parse(dto.definition)).toEqual({
      type: 'VerticalLayout',
      elements: [
        { type: 'Control', scope: '#/properties/website' },
        { type: 'Label', text: '<p>t(website) info</p>' },
        { type: 'Control', scope: '#/properties/phone', hint: 't(phone.hint)' },
      ],
    });
    // the texts of the used keys only, without the empty ones
    expect(JSON.parse(dto.translations as string)).toEqual({
      en: { 'phone.title': 'Phone', 'phone.hint': 'Digits only' },
      fr: { 'phone.hint': 'Chiffres seulement' },
    });
    // reloaded
    expect(mocked.get).toHaveBeenCalledWith('/config/network/form-custom');
  });

  it('is dirty once the builder changes the form, until it is saved', async () => {
    mockServer(asfForm);
    const { form, dirty, load, update, save } = useEntityConfigForm(target);
    await load();
    expect(dirty.value).toBe(false);
    update({ ...(form.value as FormDefinition), translations: { en: { 'website.hint': 'URL' } } });
    expect(dirty.value).toBe(true);
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(true);
    expect(dirty.value).toBe(false);
  });

  it('omits the translations when the form has no text', async () => {
    mockServer(asfForm);
    const { form, load, save } = useEntityConfigForm(target);
    await load();
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(true);
    const [, dto] = mocked.put.mock.calls[0] as [string, { translations?: string }];
    expect(dto.translations).toBeUndefined();
  });

  it('sends the project form without type, as its own DTO', async () => {
    mockServer({ schema: asfForm.schema, definition: asfForm.definition, properties: [] }, 'project');
    const { form, load, save } = useEntityConfigForm({ name: 'project' });
    await load();
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(true);
    const [url, dto] = mocked.put.mock.calls[0] as [string, Record<string, unknown>];
    expect(url).toBe('/config/project/form-custom');
    expect('type' in dto).toBe(false);
    expect(typeof dto.schema).toBe('string');
  });

  it('reports a failed save', async () => {
    mockServer(asfForm);
    const { form, load, save } = useEntityConfigForm(target);
    await load();
    mocked.put.mockRejectedValueOnce(new Error('400'));
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(false);
  });
});

describe('prepareForm', () => {
  it('leaves a literal as it is', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { phone: { title: 'Phone', type: 'string' } } },
      uischema: { type: 'VerticalLayout', elements: [{ type: 'Control', scope: '#/properties/phone' }] },
    });
    const prepared = prepareForm(model);
    expect((prepared.schema.properties as Record<string, { title: string }>).phone?.title).toBe('Phone');
    expect(prepared.translations).toEqual({});
  });

  it('keeps the texts under the prefix of a control, read by the renderers', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { phone: { title: 'phone.title', type: 'string' } }, required: ['phone'] },
      uischema: { type: 'VerticalLayout', elements: [{ type: 'Control', scope: '#/properties/phone' }] },
      translations: { en: { 'phone.title': 'Phone', 'phone.error.required': 'A phone is required', orphan: 'Unused' } },
    });
    const prepared = prepareForm(model);
    expect(prepared.schema.required).toEqual(['phone']);
    expect(prepared.translations).toEqual({
      en: { 'phone.title': 'Phone', 'phone.error.required': 'A phone is required' },
    });
  });

  it('does not touch the model of the builder', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { phone: { title: 'phone.title', type: 'string' } } },
      translations: { en: { 'phone.title': 'Phone', orphan: 'Unused' } },
    });
    prepareForm(model);
    expect(model.translations).toEqual({ en: { 'phone.title': 'Phone', orphan: 'Unused' } });
    expect((model.schema.properties as Record<string, { title: string }>).phone?.title).toBe('phone.title');
  });
});
