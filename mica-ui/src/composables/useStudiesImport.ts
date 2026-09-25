import { api } from 'src/boot/api';
import type { StudySummaryDto } from 'src/models/Mica';

/** the study types that can be imported, as the import endpoints name them */
export type ImportStudyType = 'individual-study' | 'harmonization-study';

/** the remote Mica to import from */
export interface ImportConnection {
  url: string;
  username: string;
  password: string;
}

/** a form section of the study type (study, population, data collection event), compared with the remote one */
export interface FormSectionDiff {
  section: string;
  /** the remote form has the same configuration */
  equal: boolean;
  /** equal, and so are its parent sections: its content can be imported */
  importable: boolean;
  /** 0 for the study form, 1 for its populations... */
  depth: number;
}

/** a selected study that already exists here */
export interface ExistingStudy {
  /** it exists as the other study type, it cannot be imported */
  conflict: boolean;
  localPopulationSize: number;
  localDCEsSize: number;
}

/** what the import will do with the selected studies */
export interface ImportOperations {
  toCreate: StudySummaryDto[];
  toReplace: (StudySummaryDto & ExistingStudy)[];
  conflicts: StudySummaryDto[];
}

const BASE = '/draft/studies/import';
const PROBLEMS = [204, 400, 401, 404, 408, 500, 503];

/** the i18n key of an import status: an HTTP status of the remote Mica or of a study save */
export function importStatusMessage(code: number): string {
  if (code === 200) return 'studies_import.status_ok';
  return `studies_import.problems.problem_${PROBLEMS.includes(code) ? code : 500}`;
}

/** the remote failures are served as a successful response with the status code as body: thrown as their i18n key */
function remote<T>(data: T | number): T {
  if (typeof data === 'number') throw importStatusMessage(data);
  return data;
}

/** the remote credentials travel in headers so that they never appear in URLs or access logs */
function headers(connection: ImportConnection) {
  return { 'X-Mica-Remote-Username': connection.username, 'X-Mica-Remote-Password': connection.password };
}

/** the sections from the `_differences` response, keyed by `{formSection, parentFormSection, endpoint}` JSON strings */
export function parseDifferences(data: Record<string, boolean>): FormSectionDiff[] {
  const diffs: FormSectionDiff[] = [];
  for (const [key, equal] of Object.entries(data)) {
    const { formSection, parentFormSection } = JSON.parse(key) as { formSection: string; parentFormSection: string };
    const parent = diffs.find((diff) => diff.section === parentFormSection);
    diffs.push({
      section: formSection,
      equal,
      importable: equal && (parent?.importable ?? true),
      depth: parent ? parent.depth + 1 : 0,
    });
  }
  return diffs;
}

/** the import is possible when the study form has the same configuration */
export function canImport(diffs: FormSectionDiff[]): boolean {
  return diffs[0]?.equal === true;
}

/** the sections that differ, their content (and their children's) is left out by the save */
export function differentSections(diffs: FormSectionDiff[]): string[] {
  return diffs.filter((diff) => !diff.equal).map((diff) => diff.section);
}

export function isImportable(diffs: FormSectionDiff[], section: string): boolean {
  return diffs.find((diff) => diff.section === section)?.importable === true;
}

/** splits the selected studies by the `_summary` response (`{id: "{conflict, localPopulationSize, localDCEsSize}"}`) */
export function importOperations(selected: StudySummaryDto[], summary: Record<string, string>): ImportOperations {
  const operations: ImportOperations = { toCreate: [], toReplace: [], conflicts: [] };
  for (const study of selected) {
    const value = summary[study.id];
    if (!value) {
      operations.toCreate.push(study);
      continue;
    }
    const existing = JSON.parse(value) as ExistingStudy;
    if (existing.conflict) operations.conflicts.push(study);
    else
      operations.toReplace.push({
        ...study,
        conflict: false,
        localPopulationSize: Number(existing.localPopulationSize),
        localDCEsSize: Number(existing.localDCEsSize),
      });
  }
  return operations;
}

/** the comparison of the forms of the study type with the remote ones */
export async function fetchDifferences(connection: ImportConnection, type: ImportStudyType) {
  const response = await api.get<Record<string, boolean> | number>(`${BASE}/_differences`, {
    headers: headers(connection),
    params: { url: connection.url, type },
  });
  return parseDifferences(remote(response.data));
}

/** the remote studies of the type */
export async function fetchRemoteStudies(connection: ImportConnection, type: ImportStudyType) {
  const response = await api.get<StudySummaryDto[] | number>(`${BASE}/_preview`, {
    headers: headers(connection),
    params: { url: connection.url, type },
  });
  return remote(response.data);
}

/** what the import will do with the selected studies, by the local ones */
export async function fetchImportOperations(selected: StudySummaryDto[], type: ImportStudyType) {
  const response = await api.get<Record<string, string>>(`${BASE}/_summary`, {
    params: { ids: selected.map((study) => study.id), type },
    paramsSerializer: { indexes: null },
  });
  return importOperations(selected, response.data);
}

/** imports the studies, resolves to the HTTP status of each study id */
export async function saveStudies(
  connection: ImportConnection,
  type: ImportStudyType,
  ids: string[],
  listDiffsForm: string[],
) {
  const response = await api.put<Record<string, number>>(`${BASE}/_save`, null, {
    headers: headers(connection),
    params: { url: connection.url, type, ids, listDiffsForm },
    paramsSerializer: { indexes: null },
  });
  return response.data;
}
