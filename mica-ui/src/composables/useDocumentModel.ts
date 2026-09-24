import type { MaybeRefOrGetter } from 'vue';
import type { LocalizedStringDto } from 'src/models/Mica';
import type { DocumentType } from 'src/composables/useDocumentTarget';

/** a DTO with a `content` (the custom part of the document as a JSON string) */
export interface ModelledDocument {
  content?: string | undefined;
}

export type FormModel = Record<string, unknown>;

/**
 * The DTO fields that the mandatory part of the form edits as `_<field>` model keys, as the legacy
 * admin app did: they are not part of the `content`. The localized ones (`[{lang, value}]`) become
 * localized strings (`{lang: value}`), the plain ones are copied as they are.
 */
export interface MandatoryFields {
  localized: string[];
  plain: string[];
}

export const MANDATORY_FIELDS: Record<DocumentType, MandatoryFields> = {
  network: { localized: ['name', 'acronym', 'description'], plain: [] },
  'individual-study': { localized: ['name', 'acronym', 'objectives'], plain: ['opal'] },
  'harmonization-study': { localized: ['name', 'acronym', 'objectives'], plain: ['opal'] },
  'collected-dataset': { localized: ['name', 'acronym', 'description'], plain: ['entityType'] },
  'harmonized-dataset': { localized: ['name', 'acronym', 'description'], plain: ['entityType'] },
  project: { localized: ['title', 'summary'], plain: [] },
};

/** the fields of the populations and of their data collection events, edited as the documents ones */
export const POPULATION_FIELDS: MandatoryFields = { localized: ['name', 'description'], plain: ['id'] };
export const DCE_FIELDS: MandatoryFields = {
  localized: ['name', 'description'],
  plain: ['id', 'startYear', 'startMonth', 'startDay', 'endYear', 'endMonth', 'endDay'],
};

export function modelKey(field: string) {
  return `_${field}`;
}

export function localizedToObject(values: LocalizedStringDto[] | undefined): Record<string, string> {
  const result: Record<string, string> = {};
  (values ?? []).forEach((entry) => {
    if (entry.lang) result[entry.lang] = entry.value ?? '';
  });
  return result;
}

export function localizedToArray(values: unknown): LocalizedStringDto[] | undefined {
  if (typeof values !== 'object' || values === null) return undefined;
  const entries = Object.entries(values as Record<string, unknown>)
    .filter(([, value]) => typeof value === 'string')
    .map(([lang, value]) => ({ lang, value: value as string }));
  return entries.length === 0 ? undefined : entries;
}

/** the form model of a document: its `content` plus the mandatory fields as `_<field>` */
export function toModel(document: ModelledDocument, fields: MandatoryFields): FormModel {
  const record = document as unknown as Record<string, unknown>;
  const model: FormModel = document.content ? JSON.parse(document.content) : {};
  fields.localized.forEach((field) => {
    const values = localizedToObject(record[field] as LocalizedStringDto[] | undefined);
    // an empty field is left out so that the schema `required` applies (not the "completed" check)
    if (Object.keys(values).length > 0) model[modelKey(field)] = values;
  });
  fields.plain.forEach((field) => {
    if (record[field] !== undefined && record[field] !== null && record[field] !== '')
      model[modelKey(field)] = record[field];
  });
  return model;
}

/**
 * A copy of the document updated from the form model: the localized fields are written back as
 * arrays, the plain ones as they are, the rest of the model becomes the `content`.
 */
export function fromModel<T extends ModelledDocument>(document: T, model: FormModel, fields: MandatoryFields): T {
  const content: FormModel = { ...model };
  const updated: Record<string, unknown> = { ...(document as unknown as Record<string, unknown>) };
  fields.localized.forEach((field) => {
    updated[field] = localizedToArray(content[modelKey(field)]);
    delete content[modelKey(field)];
  });
  fields.plain.forEach((field) => {
    const value = content[modelKey(field)];
    updated[field] = value === '' ? undefined : value;
    delete content[modelKey(field)];
  });
  updated.content = JSON.stringify(content);
  return updated as T;
}

/** the DTO <-> form model mapping of a document type */
export function useDocumentModel(type: MaybeRefOrGetter<DocumentType>) {
  const fields = () => MANDATORY_FIELDS[toValue(type)];
  return {
    fields: computed(fields),
    toModel: (document: ModelledDocument) => toModel(document, fields()),
    fromModel: <T extends ModelledDocument>(document: T, model: FormModel) => fromModel(document, model, fields()),
  };
}
