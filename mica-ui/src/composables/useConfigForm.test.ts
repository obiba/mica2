import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { fromDefinition } from '@obiba/quasar-ui-json-form/builder';
import type { FormDefinition } from '@obiba/quasar-ui-json-form/builder';
import { api } from 'src/boot/api';
import { EntityFormDto_Type } from 'src/models/Mica';
import {
  parseTranslations,
  prepareForm,
  seedTranslations,
  untranslatedTokens,
  useConfigForm,
  type ConfigFormDto,
} from './useConfigForm';
import { useEntityConfigForm, type EntityConfigTarget } from './useEntityConfigForm';

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

/** the Mica bundles, nested as the server sends them */
const bundles: Record<string, object> = {
  en: { website: 'Website', info: { title: 'Information' } },
  fr: { website: 'Site web', info: { title: 'Information (fr)' } },
};

function mockServer(dto: object, name = 'network') {
  mocked.get.mockImplementation((url: string) => {
    if (url === `/config/${name}/form-custom`) return Promise.resolve({ data: dto });
    const bundle = /^\/config\/i18n\/(\w+)\.json$/.exec(url);
    if (bundle) return Promise.resolve({ data: bundles[bundle[1] as string] ?? {} });
    return Promise.reject(new Error(`unexpected ${url}`));
  });
  mocked.put.mockResolvedValue({ status: 200 });
}

/** the calls of the bundles */
function bundleCalls(): string[] {
  return mocked.get.mock.calls.map((call) => call[0] as string).filter((url) => url.includes('/i18n/'));
}

beforeEach(() => {
  setActivePinia(createPinia());
  useSystemStore().configuration.languages = ['en', 'fr'];
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

describe('useConfigForm', () => {
  it('loads a legacy form, its bundle tokens seeded into the texts of the builder', async () => {
    mockServer(asfForm);
    const { form, load, dirty, legacy } = useEntityConfigForm(target);
    await load();
    expect(bundleCalls()).toEqual(['/config/i18n/en.json', '/config/i18n/fr.json']);
    const loaded = form.value as FormDefinition;
    // a single token keeps its key, translated from the bundles
    expect((loaded.schema.properties as Record<string, { title: string }>).website?.title).toBe('website');
    const elements = (loaded.uischema as { elements: Record<string, unknown>[] }).elements;
    expect(elements[0]).toEqual({ type: 'Control', scope: '#/properties/website' });
    // an embedded token: the text is owned by the builder, resolved in every language
    const textKey = elements[1]?.text as string;
    expect(textKey).not.toContain('t(');
    expect(loaded.translations).toEqual({
      en: { website: 'Website', [textKey]: '<p>Website info</p>' },
      fr: { website: 'Site web', [textKey]: '<p>Site web info</p>' },
    });
    expect(legacy.value).toBe(true);
    // an entity form in the legacy dialect is not a change by itself
    expect(dirty.value).toBe(false);
  });

  it('keeps the tokens of the keys no bundle knows, and does not fetch the bundles of a translated form', async () => {
    mockServer({
      ...asfForm,
      schema: JSON.stringify({ type: 'object', properties: { website: { title: 't(unknown.key)', type: 'string' } } }),
      definition: JSON.stringify(['website', { type: 'help', helpvalue: '<p>t(unknown.key) t(website)</p>' }]),
    });
    const { form, load } = useEntityConfigForm(target);
    await load();
    const loaded = form.value as FormDefinition;
    expect((loaded.schema.properties as Record<string, { title: string }>).website?.title).toBe('t(unknown.key)');
    const elements = (loaded.uischema as { elements: Record<string, unknown>[] }).elements;
    const textKey = elements[1]?.text as string;
    expect(loaded.translations?.en?.[textKey]).toBe('<p>t(unknown.key) Website</p>');

    vi.clearAllMocks();
    mockServer(jsonForm);
    await load();
    expect(bundleCalls()).toEqual([]);
  });

  it('loads a form saved by the builder with its keys and texts, flat', async () => {
    mockServer(jsonForm);
    const { form, load, legacy } = useEntityConfigForm(target);
    await load();
    expect(legacy.value).toBe(false);
    const loaded = form.value as FormDefinition;
    const properties = loaded.schema.properties as Record<string, { title: string }>;
    // the one key of the bundle the form did not translate yet is seeded
    expect(properties.website?.title).toBe('website');
    expect(properties.phone?.title).toBe('phone.title');
    const elements = (loaded.uischema as { elements: Record<string, unknown>[] }).elements;
    expect(elements[1]?.hint).toBe('phone.hint');
    expect(loaded.translations).toEqual({
      en: { 'phone.title': 'Phone', 'phone.hint': 'Digits only', website: 'Website' },
      fr: { 'phone.title': 'Téléphone', website: 'Site web' },
    });
  });

  it('saves the form as a JSON Forms pair with its texts, and reloads', async () => {
    mockServer(asfForm);
    const { form, load, save } = useEntityConfigForm(target);
    await load();
    const loaded = form.value as FormDefinition;
    // the seeded help block is a text of the builder now
    const textKey = (loaded.uischema as { elements: { text: string }[] }).elements[1]?.text as string;
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
        en: { 'phone.title': 'Phone', 'phone.hint': 'Digits only', orphan: 'Unused', [textKey]: '<p>Website info</p>' },
        fr: { 'phone.title': '', 'phone.hint': 'Chiffres seulement', [textKey]: '<p>Site web info</p>' },
      },
    };
    mocked.get.mockClear();
    expect(await save(fromDefinition(edited))).toBe(true);
    // the form stays as loaded (the seeded texts are known keys now)

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
        { type: 'Label', text: `t(${textKey})` },
        { type: 'Control', scope: '#/properties/phone', hint: 't(phone.hint)' },
      ],
    });
    // the texts of the used keys only, without the empty ones
    expect(JSON.parse(dto.translations as string)).toEqual({
      en: { 'phone.title': 'Phone', 'phone.hint': 'Digits only', [textKey]: '<p>Website info</p>' },
      fr: { 'phone.hint': 'Chiffres seulement', [textKey]: '<p>Site web info</p>' },
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
    mockServer({
      ...asfForm,
      schema: JSON.stringify({ type: 'object', properties: { website: { type: 'string' } } }),
      definition: '["website"]',
    });
    const { form, load, save } = useEntityConfigForm(target);
    await load();
    expect(bundleCalls()).toEqual([]);
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(true);
    const [, dto] = mocked.put.mock.calls[0] as [string, { translations?: string }];
    expect(dto.translations).toBeUndefined();
  });

  it('sends the seeded texts of a legacy form on save', async () => {
    mockServer(asfForm);
    const { form, load, save } = useEntityConfigForm(target);
    await load();
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(true);
    const [, dto] = mocked.put.mock.calls[0] as [string, { schema: string; definition: string; translations: string }];
    expect(JSON.parse(dto.schema).properties.website.title).toBe('t(website)');
    const translations = JSON.parse(dto.translations) as Record<string, Record<string, string>>;
    expect(translations.en?.website).toBe('Website');
    expect(translations.fr?.website).toBe('Site web');
    const text = (JSON.parse(dto.definition).elements[1] as { text: string }).text;
    expect(translations.en?.[text.slice(2, -1)]).toBe('<p>Website info</p>');
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

  it('sends the other fields of the DTO back, with the load parameters and the published revision of a form with revisions', async () => {
    const draft = {
      ...jsonForm,
      type: undefined,
      titleFieldPath: 'title',
      pdfDownloadType: 'Template',
      revision: 0,
      lastUpdateDate: '2026-09-21T10:00:00',
    };
    const latest = { ...draft, revision: 3, lastUpdateDate: '2026-09-20T10:00:00' };
    mocked.get.mockImplementation((url: string, config?: { params?: { revision?: string } }) =>
      url === '/config/data-access-form'
        ? Promise.resolve({ data: config?.params?.revision === 'latest' ? latest : draft })
        : Promise.reject(new Error(`unexpected ${url}`)),
    );
    mocked.put.mockResolvedValue({ status: 200 });
    interface DraftDto extends ConfigFormDto {
      titleFieldPath: string;
      pdfDownloadType: string;
    }
    const { form, dto, load, save, publish, published, canPublish, legacy, dirty } = useConfigForm<DraftDto>({
      path: '/config/data-access-form',
      params: { revision: 'draft' },
      payload: (loaded) => ({ titleFieldPath: loaded.titleFieldPath, pdfDownloadType: loaded.pdfDownloadType }),
      revisions: true,
      legacyIsDirty: true,
    });
    await load();
    expect(mocked.get).toHaveBeenCalledWith('/config/data-access-form', { params: { revision: 'draft' } });
    expect(mocked.get).toHaveBeenCalledWith('/config/data-access-form', { params: { revision: 'latest' } });
    expect(dto.value?.titleFieldPath).toBe('title');
    expect(published.value).toEqual({ revision: 3, lastUpdateDate: '2026-09-20T10:00:00' });
    expect(canPublish.value).toBe(true);
    expect(legacy.value).toBe(false);
    expect(dirty.value).toBe(false);

    (dto.value as DraftDto).titleFieldPath = 'projectTitle';
    expect(await save(fromDefinition(form.value as FormDefinition))).toBe(true);
    const [, payload] = mocked.put.mock.calls[0] as [string, Record<string, unknown>];
    expect(payload.titleFieldPath).toBe('projectTitle');
    expect(payload.pdfDownloadType).toBe('Template');
    expect('revision' in payload).toBe(false);
    expect(typeof payload.schema).toBe('string');

    latest.lastUpdateDate = draft.lastUpdateDate;
    expect(await publish()).toBe(true);
    expect(mocked.put).toHaveBeenLastCalledWith('/config/data-access-form/_publish');
    expect(canPublish.value).toBe(false);
  });

  it('reports a legacy form as changed when the source asks for it', async () => {
    mockServer({ schema: asfForm.schema, definition: asfForm.definition, revision: 0 }, 'data-access-form');
    mocked.get.mockImplementation((url: string, config?: { params?: { revision?: string } }) => {
      if (url === '/config/data-access-form')
        return Promise.resolve({
          data: {
            schema: asfForm.schema,
            definition: asfForm.definition,
            revision: config?.params?.revision === 'latest' ? 1 : 0,
          },
        });
      return Promise.resolve({ data: bundles[url.includes('/fr.') ? 'fr' : 'en'] });
    });
    const { load, dirty, legacy } = useConfigForm({
      path: '/config/data-access-form',
      payload: () => ({}),
      revisions: true,
      legacyIsDirty: true,
    });
    await load();
    expect(legacy.value).toBe(true);
    expect(dirty.value).toBe(true);
  });
});

describe('seedTranslations', () => {
  const flat = { en: { website: 'Website', 'info.title': 'Information' }, fr: { website: 'Site web' } };

  it('lists the tokens the form does not translate', () => {
    expect(
      untranslatedTokens({
        schema: JSON.parse(asfForm.schema),
        uischema: { type: 'Label', text: '<p>t(website) t(info.title)</p>' },
        translations: { en: { website: 'W' } },
      }),
    ).toEqual(['info.title']);
    expect(
      untranslatedTokens({
        schema: JSON.parse(jsonForm.schema),
        translations: parseTranslations(jsonForm.translations),
      }),
    ).toEqual(['website']);
  });

  it('seeds the keys of single tokens in the languages that have them', () => {
    const model = fromDefinition({
      schema: {
        type: 'object',
        properties: { website: { title: 't(website)', description: 't(info.title)', type: 'string' } },
      },
      uischema: { type: 'VerticalLayout', elements: [{ type: 'Control', scope: '#/properties/website' }] },
    });
    expect(seedTranslations(model, flat)).toBe(2);
    expect(model.translations).toEqual({
      en: { website: 'Website', 'info.title': 'Information' },
      fr: { website: 'Site web' },
    });
  });

  it('gives an embedded token its own key, and leaves the unknown ones', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { website: { type: 'string' } } },
      uischema: {
        type: 'VerticalLayout',
        elements: [
          { type: 'Label', text: '<h2>t(info.title)</h2><p>t(website), t(nope)</p>' },
          { type: 'Label', text: '<p>t(nope)</p>' },
        ],
      },
    });
    expect(seedTranslations(model, flat)).toBe(1);
    const key = model.root.children[0]?.element.text as string;
    expect(key).not.toContain('t(');
    expect(model.translations.en?.[key]).toBe('<h2>Information</h2><p>Website, t(nope)</p>');
    expect(model.translations.fr?.[key]).toBe('<h2>t(info.title)</h2><p>Site web, t(nope)</p>');
    expect(model.root.children[1]?.element.text).toBe('<p>t(nope)</p>');
  });

  it('leaves a form translating its own keys alone', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { website: { title: 't(website)', type: 'string' } } },
      translations: { en: { website: 'Mine' } },
    });
    expect(seedTranslations(model, flat)).toBe(0);
    expect(model.translations).toEqual({ en: { website: 'Mine' } });
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

  it('keeps the texts of the keys the builder has no text slot for', () => {
    const model = fromDefinition({
      schema: { type: 'object', properties: { start: { title: 'start.title', type: 'string', format: 'date' } } },
      uischema: {
        type: 'VerticalLayout',
        elements: [{ type: 'Control', scope: '#/properties/start', options: { validationMessage: 'date-error' } }],
      },
      translations: { en: { 'start.title': 'Start', 'date-error': 'Invalid date', orphan: 'Unused' } },
    });
    const prepared = prepareForm(model);
    const control = (prepared.uischema.elements as { options: { validationMessage: string } }[])[0];
    expect(control?.options.validationMessage).toBe('t(date-error)');
    expect(prepared.translations).toEqual({ en: { 'start.title': 'Start', 'date-error': 'Invalid date' } });
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
