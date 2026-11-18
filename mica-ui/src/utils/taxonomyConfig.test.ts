import { describe, expect, it } from 'vitest';
import type { VocabularyDto } from 'src/models/Opal';
import {
  getAttribute,
  rangeError,
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
    const result = toVocabulary(
      form,
      vocabulary('age', { facet: 'true', facetPosition: '2', facetExpanded: 'true', custom: 'x' }),
    );
    expect(getAttribute(result, 'alias')).toBe('model-age-value-range');
    expect(getAttribute(result, 'range')).toBe('true');
    expect(getAttribute(result, 'facetPosition')).toBeUndefined();
    expect(getAttribute(result, 'custom')).toBe('x');
    expect(result.title).toEqual([{ locale: 'en', text: 'Age' }]);
  });

  it('applies the range to the numbers only', () => {
    const result = toVocabulary({ ...vocabularyForm(), name: 'v', field: 'model.v', type: 'keyword', range: true });
    expect(getAttribute(result, 'range')).toBeUndefined();
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

describe('taxonomyConfig review fixes', () => {
  it('keeps a stored alias while the field and the range are unchanged', () => {
    const access = vocabulary('access_data', { field: 'model.access.access_data', alias: 'model-access-data' });
    const form = { ...vocabularyForm(access), title: { en: 'Data access' } };
    expect(getAttribute(toVocabulary(form, access), 'alias')).toBe('model-access-data');
    const moved = { ...form, field: 'model.access.other' };
    expect(getAttribute(toVocabulary(moved, access), 'alias')).toBe('model-access-other');
  });

  it('leaves an unchanged vocabulary as it is', () => {
    const design: VocabularyDto = {
      name: 'design',
      title: [{ locale: 'en', text: 'Design' }],
      attributes: [
        { key: 'field', value: 'model.methods.design' },
        { key: 'alias', value: 'model-methods-design' },
        { key: 'facet', value: 'true' },
        { key: 'type', value: 'keyword' },
        { key: 'facetPosition', value: '0' },
        { key: 'facetExpanded', value: 'false' },
        { key: 'forClassName', value: 'Study' },
      ],
      terms: [{ name: 'cohort_study' }],
    };
    expect(toVocabulary(vocabularyForm(design), design)).toEqual(design);
    const id = vocabulary('id', { static: 'true', type: 'keyword' });
    expect(toVocabulary(vocabularyForm(id), id)).toEqual(id);
  });

  it('checks the range bounds', () => {
    const cleared = '' as unknown as null;
    expect(rangeName(cleared, 20)).toBe('*:20');
    expect(rangeError(null, cleared, 'integer')).toBe('taxonomies.edit.range_bounds_required');
    expect(rangeError(50, 10, 'integer')).toBe('taxonomies.edit.range_order');
    expect(rangeError(1.5, 10, 'integer')).toBe('taxonomies.edit.range_integer');
    expect(rangeError(1.5, 10, 'decimal')).toBeUndefined();
    expect(rangeError(null, 10, 'integer')).toBeUndefined();
  });
});
