import type { MaybeRefOrGetter } from 'vue';
import { api } from 'src/boot/api';
import { t } from 'src/boot/i18n';
import { notifyError, notifySuccess } from 'src/utils/notify';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import type { RevisionStatus } from 'src/composables/useDocumentState';

/** what is published along with the document */
export type CascadingScope = 'NONE' | 'UNDER_REVIEW' | 'ALL';

export type DocumentAction =
  | { type: 'publish' }
  | { type: 'unpublish' }
  | { type: 'delete' }
  | { type: 'status'; status: RevisionStatus };

/** `updated`: the document changed and should be fetched again; `deleted`: it is gone; `failed`: an error was notified */
export type DocumentActionResult = 'updated' | 'deleted' | 'failed';

/**
 * The state-changing operations of a draft document (publication, status, deletion, indexing).
 * Errors are notified; the functions resolve to whether the document still exists.
 */
export function useDocumentActions(target: MaybeRefOrGetter<DocumentTarget>) {
  const busy = ref(false);

  function path() {
    return toValue(target).path;
  }

  /** whether some files of the document are under review, in which case they are published along */
  async function hasFilesUnderReview(): Promise<boolean> {
    const response = await api.get(`/draft/files-search${toValue(target).filesPath}`, {
      params: { recursively: true, query: 'revisionStatus:UNDER_REVIEW' },
    });
    return Array.isArray(response.data) && response.data.length > 0;
  }

  async function run(operation: () => Promise<unknown>, onDone: DocumentActionResult = 'updated'): Promise<DocumentActionResult> {
    busy.value = true;
    try {
      await operation();
      return onDone;
    } catch (error) {
      notifyError(error);
      return 'failed';
    } finally {
      busy.value = false;
    }
  }

  function publish(cascading?: CascadingScope) {
    return run(async () => {
      const scope = cascading ?? ((await hasFilesUnderReview()) ? 'UNDER_REVIEW' : 'NONE');
      await api.put(`${path()}/_publish`, null, { params: { cascading: scope } });
      notifySuccess('document.published');
    });
  }

  function unpublish() {
    return run(async () => {
      await api.delete(`${path()}/_publish`);
      notifySuccess('document.unpublished');
    });
  }

  function toStatus(status: RevisionStatus) {
    return run(() => api.put(`${path()}/_status`, null, { params: { value: status } }));
  }

  function index() {
    return run(() => api.put(`${path()}/_index`));
  }

  function remove() {
    return run(async () => {
      try {
        await api.delete(path());
        notifySuccess('document.deleted');
      } catch (error) {
        throw deleteConflictError(error);
      }
    }, 'deleted');
  }

  function apply(action: DocumentAction): Promise<DocumentActionResult> {
    switch (action.type) {
      case 'publish':
        return publish();
      case 'unpublish':
        return unpublish();
      case 'delete':
        return remove();
      case 'status':
        return toStatus(action.status);
    }
  }

  return { busy, publish, unpublish, toStatus, index, remove, apply, hasFilesUnderReview };
}

interface DeleteConflict {
  network?: string[];
  dataset?: string[];
  study?: string[];
}

/**
 * A 409 on delete lists the documents referencing this one (`{ network: [ids], dataset: [ids] }`):
 * turn it into a readable message, other errors pass through.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function deleteConflictError(error: any): unknown {
  if (error?.response?.status !== 409 || typeof error.response.data !== 'object' || error.response.data === null) {
    return error;
  }
  const conflict = error.response.data as DeleteConflict;
  const references = (['network', 'study', 'dataset'] as const)
    .filter((key) => (conflict[key] ?? []).length > 0)
    .map((key) => `${t(`document.references.${key}`)}: ${conflict[key]?.join(', ')}`)
    .join('; ');
  return references ? new Error(t('document.delete_conflict', { references })) : error;
}
