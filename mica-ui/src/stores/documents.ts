import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type {
  DataAccessRequestSummaryDto,
  DatasetDto,
  EntityStateDto,
  LocalizedStringDto,
  NetworkDto,
  ProjectDto,
  StudyDto,
  TimestampsDto,
} from 'src/models/Mica';
import type { DocumentTarget, DocumentType } from 'src/composables/useDocumentTarget';

/** the draft documents that share the pages of this app */
export type DocumentDto = NetworkDto | StudyDto | DatasetDto | ProjectDto;

/** what the documents lists give, enough for a table */
export interface DocumentSummary {
  id?: string | undefined;
  name?: LocalizedStringDto[] | undefined;
  title?: LocalizedStringDto[] | undefined;
  timestamps?: TimestampsDto | undefined;
  state?: EntityStateDto | undefined;
  /** the data access request a project comes from */
  request?: DataAccessRequestSummaryDto | undefined;
}

export interface LoadedDocument {
  document: DocumentDto;
  /** the state is served apart from the DTO for some types (studies) */
  state: EntityStateDto | undefined;
}

export const useDocumentsStore = defineStore('documents', () => {
  /** the last list fetched of each document type */
  const lists = ref<Partial<Record<DocumentType, DocumentSummary[]>>>({});

  function listOf(type: DocumentType): DocumentSummary[] {
    return lists.value[type] ?? [];
  }

  async function fetchDocuments(
    target: DocumentTarget,
    from: number = 0,
    limit: number = 1000,
    order: string = 'asc',
    sort: string = 'id',
  ) {
    const response = await api.get<DocumentSummary[] | Record<string, unknown>>(target.listPath, {
      params: { ...target.listParams, from, limit, order, sort },
    });
    const data = response.data;
    // the projects list is wrapped ({projects: [...], total}), the others are plain lists
    const list: DocumentSummary[] = Array.isArray(data)
      ? data
      : ((data[target.listKey ?? ''] as DocumentSummary[] | undefined) ?? []);
    lists.value[target.type] = list;
    return list;
  }

  async function fetchState(target: DocumentTarget): Promise<EntityStateDto | undefined> {
    if (!target.statePath) return undefined;
    return (await api.get<DocumentSummary>(target.statePath)).data.state;
  }

  async function fetchDocument(target: DocumentTarget): Promise<LoadedDocument> {
    const document = (await api.get<DocumentDto>(target.path)).data;
    const state = 'state' in document && document.state ? document.state : await fetchState(target);
    return { document, state };
  }

  /** a document to be created */
  function newDocument(type: DocumentType): DocumentDto {
    switch (type) {
      case 'network':
        return {
          name: [],
          acronym: [],
          description: [],
          investigators: [],
          contacts: [],
          attachments: [],
          studyIds: [],
          studySummaries: [],
          memberships: [],
          networkIds: [],
          networkSummaries: [],
          membershipSortOrder: [],
          published: false,
        };
      case 'individual-study':
      case 'harmonization-study':
        return {
          name: [],
          acronym: [],
          objectives: [],
          populations: [],
          memberships: [],
          membershipSortOrder: [],
          attributes: [],
          published: false,
        };
      case 'collected-dataset':
      case 'harmonized-dataset':
        return { name: [], acronym: [], description: [], entityType: 'Participant', attributes: [], published: false };
      case 'project':
        return { title: [], summary: [], published: false };
    }
  }

  /** creates the document, resolves to its id */
  async function createDocument(target: DocumentTarget, dto: DocumentDto): Promise<string> {
    const response = await api.post(target.collectionPath, dto);
    const location: string = response.headers['location'] ?? '';
    return location.substring(location.lastIndexOf('/') + 1);
  }

  /** saves the document, the response is the one of the type (a study one tells the potential conflicts) */
  async function saveDocument(
    target: DocumentTarget,
    dto: DocumentDto,
    comment?: string,
    params: Record<string, string | boolean> = {},
  ): Promise<unknown> {
    return (await api.put(target.path, dto, { params: comment ? { ...params, comment } : params })).data;
  }

  return {
    lists,
    listOf,
    fetchDocuments,
    fetchDocument,
    fetchState,
    newDocument,
    createDocument,
    saveDocument,
  };
});
