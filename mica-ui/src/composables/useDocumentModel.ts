import type { LocalizedStringDto } from 'src/models/Mica';
import type { DocumentType } from 'src/composables/useDocumentTarget';

/** a DTO with a `content` (the custom part of the document as a JSON string) */
export interface ModelledDocument {
  content?: string | undefined;
}

export type FormModel = Record<string, unknown>;

/**
 * The localized DTO fields (`[{lang, value}]`) that the form edits as `_<field>` localized
 * strings (`{lang: value}`), as the legacy admin app did: they are part of the mandatory part of
 * the form, not of the `content`.
 */
export const LOCALIZED_FIELDS: Record<DocumentType, string[]> = {
  network: ['name', 'acronym', 'description'],
  'individual-study': ['name', 'acronym', 'objectives'],
  'harmonization-study': ['name', 'acronym', 'objectives'],
  'collected-dataset': ['name', 'acronym', 'description'],
  'harmonized-dataset': ['name', 'acronym', 'description'],
  project: ['title', 'summary'],
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

/** the form model of a document: its `content` plus the localized fields as `_<field>` */
export function toModel(document: ModelledDocument, fields: string[]): FormModel {
  const record = document as unknown as Record<string, unknown>;
  const model: FormModel = document.content ? JSON.parse(document.content) : {};
  fields.forEach((field) => {
    const values = localizedToObject(record[field] as LocalizedStringDto[] | undefined);
    // an empty field is left out so that the schema `required` applies (not the "completed" check)
    if (Object.keys(values).length > 0) model[modelKey(field)] = values;
  });
  return model;
}

/**
 * A copy of the document updated from the form model: the localized fields are written back as
 * arrays, the rest of the model becomes the `content`.
 */
export function fromModel<T extends ModelledDocument>(document: T, model: FormModel, fields: string[]): T {
  const content: FormModel = { ...model };
  const updated: Record<string, unknown> = { ...(document as unknown as Record<string, unknown>) };
  fields.forEach((field) => {
    updated[field] = localizedToArray(content[modelKey(field)]);
    delete content[modelKey(field)];
  });
  updated.content = JSON.stringify(content);
  return updated as T;
}

/** the DTO <-> form model mapping of a document type */
export function useDocumentModel(type: DocumentType) {
  const fields = LOCALIZED_FIELDS[type];
  return {
    fields,
    toModel: (document: ModelledDocument) => toModel(document, fields),
    fromModel: <T extends ModelledDocument>(document: T, model: FormModel) => fromModel(document, model, fields),
  };
}
