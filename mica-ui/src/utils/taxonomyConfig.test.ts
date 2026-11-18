import { describe, expect, it } from 'vitest';
import type { VocabularyDto } from 'src/models/Opal';
import {
  getAttribute,
  rangeName,
  schemaFields,
  taxonomyError,
  termForm,
  toTerm,
  toVocabulary,
  vocabularyError,
  vocabularyForm,
} from 'src/utils/taxonomyConfig';

function vocabulary(name: string, attributes: Record<string, string>, terms: string[] = []): VocabularyDto {
  return {
    name,
    attributes: Object.entries(attributes).map(([key, value]) => ({ key, value })),
    terms: terms.map((term) => ({ name: term })),
  };
}

describe('taxonomyConfig', () => {
  it('derives the alias and drops the facet attributes', () => {
    const form = {
      ...vocabularyForm(),
      name: 'age',
      title: { en: 'Age', fr: ' ' },
      field: 'model.age.value',
      type: 'integer' as const,
      range: true,
      facet: false,
    };
    const result = toVocabulary(form, vocabulary('age', { facetPosition: '2', facetExpanded: 'true', custom: 'x' }));
    expect(getAttribute(result, 'alias')).toBe('model-age-value-range');
    expect(getAttribute(result, 'range')).toBe('true');
    expect(getAttribute(result, 'facetPosition')).toBeUndefined();
    expect(getAttribute(result, 'custom')).toBe('x');
    expect(result.title).toEqual([{ locale: 'en', text: 'Age' }]);
  });

  it('applies the range to the numbers only', () => {
    const result = toVocabulary({ ...vocabularyForm(), name: 'v', field: 'model.v', type: 'keyword', range: true });
    expect(getAttribute(result, 'range')).toBe('false');
    expect(getAttribute(result, 'alias')).toBe('model-v');
  });

  it('keeps the definition of a static vocabulary', () => {
    const id = vocabulary('id', { static: 'true', type: 'keyword' });
    const result = toVocabulary({ ...vocabularyForm(id), name: 'other', field: 'x', type: 'string', hidden: true }, id);
    expect(result.name).toBe('id');
    expect(getAttribute(result, 'field')).toBeUndefined();
    expect(getAttribute(result, 'type')).toBe('keyword');
    expect(getAttribute(result, 'hidden')).toBe('true');
    expect(vocabularyError(result, [])).toBeUndefined();
  });

  it('checks the names, fields and aliases', () => {
    const other = vocabulary('design', { field: 'model.methods.design', alias: 'model-methods-design' });
    expect(vocabularyError(vocabulary('design', { field: 'model.x' }), [other])).toBe('taxonomies.edit.name_exists');
    expect(vocabularyError(vocabulary('x', {}), [other])).toBe('taxonomies.edit.field_required');
    expect(vocabularyError(vocabulary('x', { field: 'model.methods.design' }), [other])).toBe(
      'taxonomies.edit.alias_exists',
    );
  });

  it('checks the taxonomy as the server does', () => {
    const range = vocabulary('age', { field: 'model.age', type: 'integer', range: 'true' });
    expect(taxonomyError({ name: 'Mica_study', vocabularies: [range] })?.error).toBe(
      'taxonomies.edit.range_terms_missing',
    );
    const terms = vocabulary('age', { field: 'model.age', type: 'integer' }, ['1:5']);
    expect(taxonomyError({ name: 'Mica_study', vocabularies: [terms] })?.error).toBe('taxonomies.edit.range_missing');
    const ok = vocabulary('age', { field: 'model.age', type: 'integer', range: 'true' }, ['*:5']);
    expect(taxonomyError({ name: 'Mica_study', vocabularies: [ok] })).toBeUndefined();
  });

  it('reads and writes the range terms', () => {
    expect(termForm({ name: '0:*' })).toMatchObject({ from: 0, to: null });
    expect(rangeName(0, null)).toBe('0:*');
    expect(toTerm({ ...termForm(), from: 18, to: 65 }, true).name).toBe('18:65');
  });

  it('lists the form fields', () => {
    const schema = {
      properties: {
        _name: { type: 'object' },
        website: { type: 'string' },
        methods: { type: 'object', properties: { design: { type: 'string' } } },
      },
    };
    expect(schemaFields(schema, 'model.')).toEqual(['model.website', 'model.methods.design']);
  });
});
