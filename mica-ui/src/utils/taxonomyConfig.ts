import type { AttributeDto, LocaleTextDto, TaxonomyDto, TermDto, VocabularyDto } from 'src/models/Opal';

/**
 * The rules of the Mica entity taxonomies (`/config/{target}/taxonomy`) edition, ported from the
 * legacy `entity-taxonomy-config-service.js`: how the vocabulary attributes drive the index mapping
 * and the search.
 */

export const VOCABULARY_TYPES = ['string', 'keyword', 'integer', 'decimal'] as const;
export type VocabularyType = (typeof VOCABULARY_TYPES)[number];

export const TERMS_SORT_KEYS = ['', 'name', 'title'] as const;

/** the document class a vocabulary can be restricted to, per target */
export const CLASS_NAMES: Record<string, string[]> = {
  study: ['Study', 'HarmonizationStudy'],
  dataset: ['CollectedDataset', 'HarmonizedDataset'],
};

/** the forms whose fields can be searched, with the path of their model in the indexed document */
export const TARGET_FORMS: Record<string, { form: string; prefix: string }[]> = {
  network: [{ form: 'network', prefix: 'model.' }],
  study: [
    { form: 'individual-study', prefix: 'model.' },
    { form: 'harmonization-study', prefix: 'model.' },
    { form: 'population', prefix: 'populations.model.' },
    { form: 'data-collection-event', prefix: 'populations.dataCollectionEvents.model.' },
  ],
  dataset: [
    { form: 'collected-dataset', prefix: 'model.' },
    { form: 'harmonized-dataset', prefix: 'model.' },
  ],
};

/** the rebuild request of the index of the documents of a target, the variables are indexed with their dataset */
export const TARGET_INDEX_PATHS: Record<string, string> = {
  network: '/draft/networks/_index',
  study: '/draft/studies/_index',
  dataset: '/draft/datasets/_index',
  variable: '/draft/datasets/_index',
};

/** the localized texts of a form, by language */
export type LocaleTexts = Record<string, string>;

export interface VocabularyForm {
  name: string;
  title: LocaleTexts;
  description: LocaleTexts;
  field: string;
  type: VocabularyType;
  repeatable: boolean;
  localized: boolean;
  range: boolean;
  hidden: boolean;
  facet: boolean;
  facetPosition: number;
  facetExpanded: boolean;
  termsSortKey: string;
  forClassName: string;
}

export interface TermForm {
  name: string;
  title: LocaleTexts;
  description: LocaleTexts;
  /** the bounds of a term of a range vocabulary, its name is `from:to`, `*` when open */
  from: number | null;
  to: number | null;
}

export function getAttribute(entity: { attributes?: AttributeDto[] } | undefined, key: string): string | undefined {
  return entity?.attributes?.find((attribute) => attribute.key === key)?.value;
}

/** sets the attribute, removes it when the value is empty */
export function setAttribute(entity: { attributes?: AttributeDto[] }, key: string, value: string | undefined) {
  const attributes = (entity.attributes || []).filter((attribute) => attribute.key !== key);
  if (value !== undefined && value !== '') attributes.push({ key, value });
  entity.attributes = attributes;
}

function isTrue(entity: { attributes?: AttributeDto[] } | undefined, key: string) {
  return getAttribute(entity, key) === 'true';
}

/** a built-in vocabulary the application relies on: it cannot be deleted, its definition cannot change */
export function isStatic(vocabulary: VocabularyDto | undefined): boolean {
  return isTrue(vocabulary, 'static');
}

export function vocabularyType(vocabulary: VocabularyDto | undefined): VocabularyType {
  const type = getAttribute(vocabulary, 'type');
  return (VOCABULARY_TYPES as readonly string[]).includes(type || '') ? (type as VocabularyType) : 'string';
}

/** the terms of a numeric vocabulary are ranges */
export function isRangeType(type: VocabularyType): boolean {
  return type === 'integer' || type === 'decimal';
}

/** the aggregation name of the field, as the server derives it (`AggregationHelper.formatName`) */
export function aliasOf(field: string, range: boolean): string {
  return field ? field.replace(/\./g, '-') + (range ? '-range' : '') : '';
}

function vocabularyAlias(vocabulary: VocabularyDto): string {
  const field = getAttribute(vocabulary, 'field');
  if (!field) return '';
  return getAttribute(vocabulary, 'alias') || aliasOf(field, false);
}

export function toLocaleTexts(texts: LocaleTextDto[] | undefined): LocaleTexts {
  return Object.fromEntries((texts || []).map((text) => [text.locale, text.text]));
}

export function fromLocaleTexts(texts: LocaleTexts): LocaleTextDto[] {
  return Object.entries(texts)
    .filter(([, text]) => text?.trim())
    .map(([locale, text]) => ({ locale, text: text.trim() }));
}

export function vocabularyForm(vocabulary?: VocabularyDto): VocabularyForm {
  return {
    name: vocabulary?.name || '',
    title: toLocaleTexts(vocabulary?.title),
    description: toLocaleTexts(vocabulary?.description),
    field: getAttribute(vocabulary, 'field') || '',
    type: vocabulary ? vocabularyType(vocabulary) : 'keyword',
    repeatable: vocabulary?.repeatable === true,
    localized: isTrue(vocabulary, 'localized'),
    range: isTrue(vocabulary, 'range'),
    hidden: isTrue(vocabulary, 'hidden'),
    facet: isTrue(vocabulary, 'facet'),
    facetPosition: Number(getAttribute(vocabulary, 'facetPosition') || 0),
    facetExpanded: isTrue(vocabulary, 'facetExpanded'),
    termsSortKey: getAttribute(vocabulary, 'termsSortKey') || '',
    forClassName: getAttribute(vocabulary, 'forClassName') || '',
  };
}

/**
 * The vocabulary edited by the form, its other attributes and its terms kept. Only the texts and
 * the display attributes of a static vocabulary are changed.
 */
export function toVocabulary(form: VocabularyForm, vocabulary?: VocabularyDto): VocabularyDto {
  const result: VocabularyDto = JSON.parse(JSON.stringify(vocabulary || { name: form.name, terms: [] }));
  result.title = fromLocaleTexts(form.title);
  result.description = fromLocaleTexts(form.description);
  setAttribute(result, 'hidden', `${form.hidden}`);
  setAttribute(result, 'facet', `${form.facet}`);
  setAttribute(result, 'facetPosition', form.facet ? `${form.facetPosition || 0}` : undefined);
  setAttribute(result, 'facetExpanded', form.facet ? `${form.facetExpanded}` : undefined);
  if (isStatic(vocabulary)) return result;

  // the range aggregation applies to the numbers only
  const range = isRangeType(form.type) && form.range;
  const field = form.field.trim();
  result.name = form.name.trim();
  result.repeatable = form.repeatable;
  setAttribute(result, 'field', field);
  setAttribute(result, 'alias', aliasOf(field, range));
  setAttribute(result, 'type', form.type);
  setAttribute(result, 'localized', `${form.localized}`);
  setAttribute(result, 'range', `${range}`);
  setAttribute(result, 'termsSortKey', form.termsSortKey);
  setAttribute(result, 'forClassName', form.forClassName);
  return result;
}

/** the i18n key of the first rule the vocabulary breaks among its siblings, if any */
export function vocabularyError(vocabulary: VocabularyDto, others: VocabularyDto[]): string | undefined {
  if (!vocabulary.name) return 'taxonomies.edit.name_required';
  if (others.some((other) => other.name === vocabulary.name)) return 'taxonomies.edit.name_exists';
  if (isStatic(vocabulary)) return undefined;
  if (!getAttribute(vocabulary, 'field')) return 'taxonomies.edit.field_required';
  const alias = vocabularyAlias(vocabulary);
  if (others.some((other) => vocabularyAlias(other) === alias)) return 'taxonomies.edit.alias_exists';
  return undefined;
}

/** the i18n key of the first rule the taxonomy breaks, checked as the server does before saving */
export function taxonomyError(taxonomy: TaxonomyDto): { error: string; vocabulary: string } | undefined {
  const aliases = new Set<string>();
  for (const vocabulary of taxonomy.vocabularies || []) {
    const alias = vocabularyAlias(vocabulary);
    if (!alias) continue;
    if (aliases.has(alias)) return { error: 'taxonomies.edit.alias_duplicate', vocabulary: vocabulary.name };
    aliases.add(alias);
    const hasTerms = (vocabulary.terms || []).length > 0;
    const range = isTrue(vocabulary, 'range');
    if (isRangeType(vocabularyType(vocabulary)) && hasTerms !== range) {
      return {
        error: range ? 'taxonomies.edit.range_terms_missing' : 'taxonomies.edit.range_missing',
        vocabulary: vocabulary.name,
      };
    }
  }
  return undefined;
}

function parseBound(value: string | undefined): number | null {
  return value && value !== '*' && !isNaN(Number(value)) ? Number(value) : null;
}

export function rangeName(from: number | null, to: number | null): string {
  return `${from ?? '*'}:${to ?? '*'}`;
}

export function termForm(term?: TermDto): TermForm {
  const [from, to] = (term?.name || '').split(':');
  return {
    name: term?.name || '',
    title: toLocaleTexts(term?.title),
    description: toLocaleTexts(term?.description),
    from: parseBound(from),
    to: parseBound(to),
  };
}

/** the term edited by the form, its keywords, attributes and sub-terms kept */
export function toTerm(form: TermForm, range: boolean, term?: TermDto): TermDto {
  const result: TermDto = JSON.parse(JSON.stringify(term || { name: '' }));
  result.name = range ? rangeName(form.from, form.to) : form.name.trim();
  result.title = fromLocaleTexts(form.title);
  result.description = fromLocaleTexts(form.description);
  return result;
}

/** moves the item by one position, within the bounds */
export function moveItem<T>(items: T[], index: number, up: boolean) {
  const other = up ? index - 1 : index + 1;
  if (index < 0 || other < 0 || other >= items.length) return;
  [items[index], items[other]] = [items[other]!, items[index]!];
}

/** the dotted paths of the leaf fields of a form JSON schema, the mandatory ones (`_name`) excepted */
export function schemaFields(schema: Record<string, unknown> | undefined, prefix = ''): string[] {
  const properties = schema?.properties as Record<string, Record<string, unknown>> | undefined;
  if (!properties || typeof properties !== 'object') return [];
  return Object.entries(properties)
    .filter(([key]) => !key.startsWith('_'))
    .flatMap(([key, child]) => {
      const path = `${prefix}${key}`;
      return child?.properties ? schemaFields(child, `${path}.`) : [path];
    });
}
