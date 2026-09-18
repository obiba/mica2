import type { MaybeRefOrGetter } from 'vue';

/**
 * The types of documents that share the draft REST layout (`/draft/{type}/{id}`), the form
 * configuration (`/config/{type}/form`) and the file system (`/{type}/{id}`).
 */
export type DocumentType =
  | 'network'
  | 'individual-study'
  | 'harmonization-study'
  | 'collected-dataset'
  | 'harmonized-dataset'
  | 'project';

export interface DocumentTarget {
  type: DocumentType;
  id: string;
  /** REST path of the draft document, relative to the API base: `/draft/network/{id}` */
  path: string;
  /** REST path of the document collection: `/draft/networks` */
  collectionPath: string;
  /** root of the document's files in the file system: `/network/{id}` */
  filesPath: string;
  /** REST path of the form configuration: `/config/network/form` */
  formPath: string;
  /** base of the document routes in this app: `/network` */
  routeBase: string;
}

const COLLECTIONS: Record<DocumentType, string> = {
  network: 'networks',
  'individual-study': 'individual-studies',
  'harmonization-study': 'harmonization-studies',
  'collected-dataset': 'collected-datasets',
  'harmonized-dataset': 'harmonized-datasets',
  project: 'projects',
};

export function documentTarget(type: DocumentType, id: string): DocumentTarget {
  return {
    type,
    id,
    path: `/draft/${type}/${id}`,
    collectionPath: `/draft/${COLLECTIONS[type]}`,
    filesPath: `/${type}/${id}`,
    formPath: `/config/${type}/form`,
    routeBase: `/${type}`,
  };
}

/**
 * The target of a document page, recomputed when the id changes (route param).
 */
export function useDocumentTarget(type: DocumentType, id: MaybeRefOrGetter<string>) {
  const target = computed<DocumentTarget>(() => documentTarget(type, toValue(id)));
  return { target };
}
