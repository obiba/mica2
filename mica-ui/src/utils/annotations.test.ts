import { describe, expect, it } from 'vitest';
import type { AttributeDto } from 'src/models/Mica';
import type { TaxonomyDto } from 'src/models/Opal';
import {
  addAnnotations,
  annotationTaxonomies,
  groupAnnotations,
  matchesFilter,
  removeAnnotations,
  toAttributesBody,
} from './annotations';

const en = (text: string) => [{ locale: 'en', text }];

const taxonomies: TaxonomyDto[] = [
  { name: 'Mica_variable', vocabularies: [] },
  {
    name: 'AreaOfInformation',
    title: en('Area of information'),
    vocabularies: [
      { name: 'Lifestyle', title: en('Lifestyle'), description: en('Tobacco, alcohol') },
      { name: 'Diseases', title: en('Diseases') },
    ],
  },
  { name: 'Other', vocabularies: [{ name: 'x' }] },
];

const attribute = (namespace: string, name: string): AttributeDto => ({ namespace, name, values: [] });

describe('annotations', () => {
  it('lists the usable taxonomies', () => {
    expect(annotationTaxonomies(taxonomies, []).map((t) => t.name)).toEqual(['AreaOfInformation', 'Other']);
    expect(annotationTaxonomies(taxonomies, ['Other']).map((t) => t.name)).toEqual(['Other']);
  });

  it('groups the annotations in the taxonomies order', () => {
    const groups = groupAnnotations(
      [
        attribute('Unknown', 'a'),
        attribute('AreaOfInformation', 'Diseases'),
        attribute('Other', 'x'),
        attribute('AreaOfInformation', 'Gone'),
        attribute('AreaOfInformation', 'Lifestyle'),
        attribute('AreaOfInformation', 'Lifestyle'),
        { name: 'no-namespace', values: [] },
      ],
      taxonomies,
      ['AreaOfInformation'],
    );
    expect(groups.map((g) => [g.name, g.configured, g.taxonomy !== undefined])).toEqual([
      ['AreaOfInformation', true, true],
      ['Other', false, true],
      ['Unknown', false, false],
    ]);
    expect(groups[0]!.vocabularies.map((v) => [v.name, v.vocabulary !== undefined])).toEqual([
      ['Lifestyle', true],
      ['Diseases', true],
      ['Gone', false],
    ]);
  });

  it('never considers Mica_variable as configured', () => {
    expect(groupAnnotations([attribute('Mica_variable', 'x')], taxonomies, [])[0]!.configured).toBe(false);
  });

  it('adds and removes annotations', () => {
    const attributes = addAnnotations(
      [attribute('AreaOfInformation', 'Lifestyle')],
      [
        { namespace: 'AreaOfInformation', name: 'Lifestyle' },
        { namespace: 'AreaOfInformation', name: 'Diseases' },
        { namespace: 'AreaOfInformation', name: 'Diseases' },
        { namespace: 'Other', name: 'x' },
      ],
    );
    expect(attributes.map((a) => `${a.namespace}.${a.name}`)).toEqual([
      'AreaOfInformation.Lifestyle',
      'AreaOfInformation.Diseases',
      'Other.x',
    ]);
    expect(removeAnnotations(attributes, 'AreaOfInformation', 'Lifestyle').map((a) => a.name)).toEqual([
      'Diseases',
      'x',
    ]);
    expect(removeAnnotations(attributes, 'AreaOfInformation').map((a) => a.name)).toEqual(['x']);
  });

  it('makes the attributes body', () => {
    expect(toAttributesBody([{ namespace: 'n', name: 'v', values: [{ lang: 'und', value: 'a' }] }])).toEqual([
      { namespace: 'n', name: 'v', values: { und: 'a' } },
    ]);
  });

  it('filters by name, title or description', () => {
    const vocabulary = taxonomies[1]!.vocabularies![0]!;
    expect(matchesFilter(vocabulary, '', 'en')).toBe(true);
    expect(matchesFilter(vocabulary, 'lifes', 'en')).toBe(true);
    expect(matchesFilter(vocabulary, 'ALCOHOL', 'fr')).toBe(true);
    expect(matchesFilter(vocabulary, 'disease', 'en')).toBe(false);
  });
});
