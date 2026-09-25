import type { AttributeDto } from 'src/models/Mica';
import type { TaxonomyDto, TermDto, VocabularyDto } from 'src/models/Opal';
import { localeText } from 'src/utils/config';

/**
 * The annotations of a study by taxonomy vocabulary are its attributes `{namespace: taxonomy, name: vocabulary}`,
 * without value.
 */
export interface Annotation {
  namespace: string;
  name: string;
}

export interface AnnotationVocabulary {
  name: string;
  /** undefined when the vocabulary does not exist (anymore) in the taxonomy */
  vocabulary?: VocabularyDto | undefined;
}

export interface AnnotationGroup {
  name: string;
  /** undefined when the taxonomy does not exist (anymore) */
  taxonomy?: TaxonomyDto | undefined;
  /** whether the taxonomy is one of the taxonomies usable for annotation */
  configured: boolean;
  vocabularies: AnnotationVocabulary[];
}

/** the variable taxonomies usable for annotation: all of them (except `Mica_variable`) when none is configured */
export function annotationTaxonomies(taxonomies: TaxonomyDto[], configured: string[]): TaxonomyDto[] {
  return taxonomies.filter(
    (taxonomy) => taxonomy.name !== 'Mica_variable' && (configured.length === 0 || configured.includes(taxonomy.name)),
  );
}

/** the annotations grouped by taxonomy, in the taxonomies order, the unknown taxonomies and vocabularies last */
export function groupAnnotations(
  attributes: AttributeDto[],
  taxonomies: TaxonomyDto[],
  configured: string[],
): AnnotationGroup[] {
  const groups: AnnotationGroup[] = [];
  attributes.forEach((attribute) => {
    if (!attribute.namespace) return;
    let group = groups.find((item) => item.name === attribute.namespace);
    if (!group) {
      group = {
        name: attribute.namespace,
        taxonomy: taxonomies.find((taxonomy) => taxonomy.name === attribute.namespace),
        configured: configured.length === 0 || configured.includes(attribute.namespace),
        vocabularies: [],
      };
      groups.push(group);
    }
    if (group.vocabularies.some((item) => item.name === attribute.name)) return;
    group.vocabularies.push({
      name: attribute.name,
      vocabulary: group.taxonomy?.vocabularies?.find((vocabulary) => vocabulary.name === attribute.name),
    });
  });
  const rank = (names: string[], name: string) => {
    const idx = names.indexOf(name);
    return idx === -1 ? names.length : idx;
  };
  const taxonomyNames = taxonomies.map((taxonomy) => taxonomy.name);
  groups.forEach((group) => {
    const names = (group.taxonomy?.vocabularies ?? []).map((vocabulary) => vocabulary.name);
    group.vocabularies.sort((a, b) => rank(names, a.name) - rank(names, b.name));
  });
  return groups.sort((a, b) => rank(taxonomyNames, a.name) - rank(taxonomyNames, b.name));
}

export function isAnnotated(attributes: AttributeDto[], annotation: Annotation): boolean {
  return attributes.some(
    (attribute) => attribute.namespace === annotation.namespace && attribute.name === annotation.name,
  );
}

/** the attributes with the annotations not already there */
export function addAnnotations(attributes: AttributeDto[], annotations: Annotation[]): AttributeDto[] {
  const added: AttributeDto[] = [];
  annotations.forEach((annotation) => {
    if (!isAnnotated([...attributes, ...added], annotation)) added.push({ ...annotation, values: [] });
  });
  return [...attributes, ...added];
}

/** the attributes without the annotations of the taxonomy, or of one of its vocabularies */
export function removeAnnotations(attributes: AttributeDto[], namespace: string, name?: string): AttributeDto[] {
  return attributes.filter(
    (attribute) => attribute.namespace !== namespace || (name !== undefined && attribute.name !== name),
  );
}

/** the body of `PUT /draft/{type}/{id}/attributes`: the attribute values are a `{lang: value}` map */
export function toAttributesBody(attributes: AttributeDto[]) {
  return attributes.map((attribute) => ({
    ...attribute,
    values: Object.fromEntries((attribute.values ?? []).map((value) => [value.lang, value.value])),
  }));
}

/** whether the name, the title or the description (in the locale) contains the text, case insensitive */
export function matchesFilter(entity: TaxonomyDto | TermDto, filter: string, locale: string): boolean {
  const text = filter.trim().toLowerCase();
  if (!text) return true;
  return [entity.name, localeText(entity.title, locale), localeText(entity.description, locale)].some((value) =>
    value.toLowerCase().includes(text),
  );
}
