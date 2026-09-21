import { api, toPortalUrl } from 'src/boot/api';
import { documentTarget, type DocumentType } from 'src/composables/useDocumentTarget';
import type { StateFilter } from 'src/composables/useDocumentState';
import type { EntityIndexHealthDto, EntityIndexHealthDto_ItemDto, MicaMetricsDto } from 'src/models/Mica';
import { notifyError } from 'src/utils/notify';

/** the document types of the metrics (`/config/metrics`), in display order */
export const METRICS_TYPES = [
  'Study',
  'HarmonizationStudy',
  'StudyDataset',
  'HarmonizationDataset',
  'Network',
  'DatasetVariable',
  'Project',
  'DataAccessRequest',
] as const;

export type MetricsType = (typeof METRICS_TYPES)[number];

/** the types that are draft documents of this app, with a list page and a search index */
export const DOCUMENT_METRICS_TYPES: Partial<Record<MetricsType, DocumentType>> = {
  Study: 'individual-study',
  HarmonizationStudy: 'harmonization-study',
  StudyDataset: 'collected-dataset',
  HarmonizationDataset: 'harmonized-dataset',
  Network: 'network',
  Project: 'project',
};

export const DAR_STATUSES = [
  'OPENED',
  'SUBMITTED',
  'REVIEWED',
  'CONDITIONALLY_APPROVED',
  'APPROVED',
  'REJECTED',
] as const;

export interface TypeMetrics {
  type: MetricsType;
  /** the counts by name, as served: `total`, `published`, `under_review`, `in_edition`, `to_delete`, `totalFiles`... */
  counts: Record<string, number>;
  /** published documents missing from the search index (`published - indexed`), for the indexed document types */
  notIndexed?: number;
}

export interface ContentMetrics {
  types: TypeMetrics[];
  /** the sum of the documents requiring indexing, over the types */
  notIndexed: number;
}

/** the counts and the document type of a metrics type, ordered and normalized */
export function normalizeMetrics(dto: MicaMetricsDto | undefined): ContentMetrics {
  const byType = new Map((dto?.documents ?? []).map((document) => [document.type, document]));
  const types: TypeMetrics[] = [];
  METRICS_TYPES.forEach((type) => {
    const document = byType.get(type);
    if (!document) return;
    const counts: Record<string, number> = {};
    document.properties.forEach((property) => {
      counts[property.name] = property.value ?? 0;
    });
    const metrics: TypeMetrics = { type, counts };
    if ('indexed' in counts) {
      metrics.notIndexed = Math.max(0, (counts.published ?? 0) - (counts.indexed ?? 0));
    }
    types.push(metrics);
  });
  return { types, notIndexed: types.reduce((sum, metrics) => sum + (metrics.notIndexed ?? 0), 0) };
}

/** the route of the documents list of a type, filtered by state */
export function documentsListRoute(type: MetricsType, filter?: StateFilter): string | undefined {
  const documentType = DOCUMENT_METRICS_TYPES[type];
  if (!documentType) return undefined;
  const { listRoute } = documentTarget(documentType, '');
  return filter ? `${listRoute}?status=${filter}` : listRoute;
}

const STUDY_QUERY = 'study(in(Mica_study.className,Study))';
const INITIATIVE_QUERY = 'study(in(Mica_study.className,HarmonizationStudy))';

/** the portal search page listing the published documents of a type, or their variables */
export function portalSearchUrl(
  type: MetricsType,
  what: 'published' | 'variables' | 'totalWithVariable',
): string | undefined {
  let search: string | undefined;
  switch (type) {
    case 'Study':
      search =
        what === 'published'
          ? `type=studies&query=${STUDY_QUERY}`
          : what === 'variables'
            ? `type=variables&query=${STUDY_QUERY}`
            : `type=studies&query=variable(in(Mica_variable.variableType,(Collected))),${STUDY_QUERY}`;
      break;
    case 'HarmonizationStudy':
      search =
        what === 'published' ? `type=studies&query=${INITIATIVE_QUERY}` : `type=variables&query=${INITIATIVE_QUERY}`;
      break;
    case 'StudyDataset':
      search = `type=datasets&query=${STUDY_QUERY}`;
      break;
    case 'HarmonizationDataset':
      search = `type=datasets&query=${INITIATIVE_QUERY}`;
      break;
    case 'Network':
      search = 'type=networks';
      break;
    case 'DatasetVariable':
      search = 'type=variables';
      break;
  }
  return search ? toPortalUrl(`/search#lists?${search}&display=list`) : undefined;
}

/** the portal page listing the data access requests */
export function portalDataAccessesUrl(): string {
  return toPortalUrl('/data-accesses');
}

/**
 * The content metrics of the server: the counts of the documents by state, of their files and
 * variables, and the health of the search index (the published documents missing from it).
 */
export function useContentMetrics() {
  const loading = ref(false);
  const metrics = ref<ContentMetrics>();
  const refreshedAt = ref<Date>();
  /** the last load failed (search engine down...): the previous metrics, if any, are kept */
  const failed = ref(false);

  async function load(): Promise<void> {
    loading.value = true;
    try {
      const response = await api.get<MicaMetricsDto>('/config/metrics');
      metrics.value = normalizeMetrics(response.data);
      refreshedAt.value = new Date();
      failed.value = false;
    } catch (error) {
      failed.value = true;
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  /** the published documents of a type missing from the search index, sorted by id */
  async function loadIndexHealth(type: MetricsType): Promise<EntityIndexHealthDto_ItemDto[]> {
    const documentType = DOCUMENT_METRICS_TYPES[type];
    if (!documentType) return [];
    const { collectionPath } = documentTarget(documentType, '');
    try {
      const response = await api.get<EntityIndexHealthDto>(`${collectionPath.replace('/draft', '')}/index/health`);
      return (response.data.requireIndexing ?? []).slice().sort((a, b) => a.id.localeCompare(b.id));
    } catch (error) {
      notifyError(error);
      return [];
    }
  }

  /**
   * Requests the indexing of the documents of a type. The server indexes them asynchronously, so the metrics are
   * not reloaded right away: they would still report the documents as not indexed.
   */
  async function index(type: MetricsType, ids: string[]): Promise<boolean> {
    const documentType = DOCUMENT_METRICS_TYPES[type];
    if (!documentType || ids.length === 0) return false;
    const { collectionPath } = documentTarget(documentType, '');
    try {
      await api.put(`${collectionPath}/_index`, null, { params: { id: ids }, paramsSerializer: { indexes: null } });
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  return { loading, failed, metrics, refreshedAt, load, loadIndexHealth, index };
}
