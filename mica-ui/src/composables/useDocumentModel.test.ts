import { describe, expect, it } from 'vitest';
import { fromModel, localizedToArray, localizedToObject, toModel, useDocumentModel, type ModelledDocument } from './useDocumentModel';

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
    expect(toModel({ name: [{ lang: 'en', value: 'x' }] } as ModelledDocument, ['name'])).toEqual({ _name: { en: 'x' } });
    expect(toModel({ name: [] } as ModelledDocument, ['name'])).toEqual({});
    expect(fromModel({}, { _name: {} }, ['name'])).toEqual({ name: undefined, content: '{}' });
  });

  it('maps the fields of every document type', () => {
    expect(useDocumentModel('individual-study').fields).toEqual(['name', 'acronym', 'objectives']);
    expect(useDocumentModel('project').fields).toEqual(['title', 'summary']);
  });

  it('converts localized strings both ways', () => {
    expect(localizedToObject(undefined)).toEqual({});
    expect(localizedToArray('not an object')).toBeUndefined();
    expect(localizedToArray({ en: 'a', fr: 1 })).toEqual([{ lang: 'en', value: 'a' }]);
  });
});
