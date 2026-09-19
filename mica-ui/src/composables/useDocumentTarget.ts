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
  /** REST path listing the documents with their state, and its fixed parameters */
  listPath: string;
  listParams: Record<string, string>;
  /** key of the documents in the list response when it is wrapped (`{projects: [...]}`), the response is the list otherwise */
  listKey?: string;
  /** REST path serving the state of the document when the document DTO does not carry it (studies) */
  statePath?: string;
  /** root of the document's files in the file system: `/network/{id}` */
  filesPath: string;
  /** REST path of the form configuration: `/config/network/form` */
  formPath: string;
  /** base of the document routes in this app: `/network` */
  routeBase: string;
  /** route of the documents list: `/networks` */
  listRoute: string;
  /** the document has a logo */
  withLogo: boolean;
  /** i18n keys of the documents title and of the "new document" label */
  labels: { title: string; new: string };
}

interface DocumentTypeInfo {
  collection: string;
  withLogo: boolean;
  labels: { title: string; new: string };
  /** the states of the studies are served apart from the study DTO */
  stateApart?: boolean;
  /** the list response is wrapped: `{ [listKey]: [...], total, from, limit }` */
  listKey?: string;
}

const TYPES: Record<DocumentType, DocumentTypeInfo> = {
  network: { collection: 'networks', withLogo: true, labels: { title: 'networks.title', new: 'networks.new' } },
  'individual-study': {
    collection: 'individual-studies',
    withLogo: true,
    stateApart: true,
    labels: { title: 'individual.studies.title', new: 'individual.studies.new' },
  },
  'harmonization-study': {
    collection: 'harmonization-studies',
    withLogo: true,
    stateApart: true,
    labels: { title: 'harmonization.studies.title', new: 'harmonization.studies.new' },
  },
  'collected-dataset': {
    collection: 'collected-datasets',
    withLogo: false,
    labels: { title: 'individual.datasets.title', new: 'individual.datasets.new' },
  },
  'harmonized-dataset': {
    collection: 'harmonized-datasets',
    withLogo: false,
    labels: { title: 'harmonization.datasets.title', new: 'harmonization.datasets.new' },
  },
  project: {
    collection: 'projects',
    listKey: 'projects',
    withLogo: false,
    labels: { title: 'research_projects.title', new: 'research_projects.new' },
  },
};

export const DOCUMENT_TYPES = Object.keys(TYPES) as DocumentType[];

export function isDocumentType(value: unknown): value is DocumentType {
  return typeof value === 'string' && value in TYPES;
}

export function documentTarget(type: DocumentType, id: string): DocumentTarget {
  const info = TYPES[type];
  const collectionPath = `/draft/${info.collection}`;
  return {
    type,
    id,
    path: `/draft/${type}/${id}`,
    collectionPath,
    // the studies list does not carry the states, the study states one does
    listPath: info.stateApart ? '/draft/study-states' : collectionPath,
    listParams: info.stateApart ? { type } : {},
    ...(info.stateApart ? { statePath: `/draft/study-state/${id}` } : {}),
    ...(info.listKey ? { listKey: info.listKey } : {}),
    filesPath: `/${type}/${id}`,
    formPath: `/config/${type}/form`,
    routeBase: `/${type}`,
    listRoute: `/${info.collection}`,
    withLogo: info.withLogo,
    labels: info.labels,
  };
}

/**
 * The target of a document page, recomputed when the type or the id changes (route).
 */
export function useDocumentTarget(type: MaybeRefOrGetter<DocumentType>, id: MaybeRefOrGetter<string>) {
  const target = computed<DocumentTarget>(() => documentTarget(toValue(type), toValue(id)));
  return { target };
}

declare module 'vue-router' {
  interface RouteMeta {
    /** the type of the documents a route serves */
    documentType?: DocumentType;
  }
}

/** the document type of the current route (`meta.documentType`) */
export function useRouteDocumentType() {
  const route = useRoute();
  return computed<DocumentType>(() => {
    const type = route.meta.documentType;
    if (!isDocumentType(type)) throw new Error(`Route ${route.path} has no document type`);
    return type;
  });
}
