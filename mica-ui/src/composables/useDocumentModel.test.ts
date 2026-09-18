import { describe, expect, it } from 'vitest';
import type { LocalizedStringDto } from 'src/models/Mica';
import {
  fromModel,
  localizedToArray,
  localizedToObject,
  toModel,
  useDocumentModel,
  type ModelledDocument,
} from './useDocumentModel';

const network = {
  id: 'net1',
  name: [
    { lang: 'en', value: 'Network' },
    { lang: 'fr', value: 'Réseau' },
  ],
  acronym: [{ lang: 'en', value: 'NET' }],
  description: [],
  content: '{"website":"https://example.org","maelstromAuthorization":{"authorized":false}}',
  studyIds: ['s1'],
};

describe('useDocumentModel', () => {
  it('builds the form model from the content and the localized fields', () => {
    const { toModel } = useDocumentModel('network');
    expect(toModel(network)).toEqual({
      website: 'https://example.org',
      maelstromAuthorization: { authorized: false },
      _name: { en: 'Network', fr: 'Réseau' },
      _acronym: { en: 'NET' },
    });
  });

  it('writes the model back into a copy of the document', () => {
    const { fromModel } = useDocumentModel('network');
    const updated = fromModel(network, {
      website: 'https://example.com',
      _name: { en: 'Renamed', fr: '' },
      _acronym: { en: 'NET' },
      _description: {},
    });
    expect(updated).not.toBe(network);
    expect(updated.name).toEqual([
      { lang: 'en', value: 'Renamed' },
      { lang: 'fr', value: '' },
    ]);
    expect(updated.acronym).toEqual([{ lang: 'en', value: 'NET' }]);
    expect(updated.description).toBeUndefined();
    expect(JSON.parse(updated.content as string)).toEqual({ website: 'https://example.com' });
    expect(updated.studyIds).toEqual(['s1']);
    // the source is untouched
    expect(network.content).toContain('example.org');
  });

  it('handles a document without content', () => {
    const fields = { localized: ['name'], plain: [] };
    expect(toModel({ name: [{ lang: 'en', value: 'x' }] } as ModelledDocument, fields)).toEqual({ _name: { en: 'x' } });
    expect(toModel({ name: [] } as ModelledDocument, fields)).toEqual({});
    expect(fromModel({}, { _name: {} }, fields)).toEqual({ name: undefined, content: '{}' });
  });

  it('maps the plain mandatory fields of studies and datasets', () => {
    const { toModel, fromModel } = useDocumentModel('individual-study');
    const study: ModelledDocument & { name?: LocalizedStringDto[]; opal?: string } = {
      name: [{ lang: 'en', value: 'S' }],
      opal: 'https://opal.example.org',
      content: '{"x":1}',
    };
    expect(toModel(study)).toEqual({ x: 1, _name: { en: 'S' }, _opal: 'https://opal.example.org' });
    delete study.opal;
    expect(toModel(study)).toEqual({ x: 1, _name: { en: 'S' } });
    const updated = fromModel(study, { x: 2, _name: { en: 'S' }, _opal: '' });
    expect(updated.opal).toBeUndefined();
    expect(JSON.parse(updated.content as string)).toEqual({ x: 2 });

    const dataset = useDocumentModel('collected-dataset');
    expect(dataset.fields.value).toEqual({ localized: ['name', 'acronym', 'description'], plain: ['entityType'] });
    const collected: ModelledDocument & { entityType: string } = { entityType: 'Participant' };
    expect(dataset.fromModel(collected, { _entityType: 'Sample' }).entityType).toBe('Sample');
  });

  it('maps the fields of every document type', () => {
    expect(useDocumentModel('harmonization-study').fields.value).toEqual({
      localized: ['name', 'acronym', 'objectives'],
      plain: ['opal'],
    });
    expect(useDocumentModel('project').fields.value).toEqual({ localized: ['title', 'summary'], plain: [] });
  });

  it('converts localized strings both ways', () => {
    expect(localizedToObject(undefined)).toEqual({});
    expect(localizedToArray('not an object')).toBeUndefined();
    expect(localizedToArray({ en: 'a', fr: 1 })).toEqual([{ lang: 'en', value: 'a' }]);
  });
});
