import type { MaybeRefOrGetter } from 'vue';
import { api } from 'src/boot/api';
import { t } from 'src/boot/i18n';
import type { GitCommitInfoDto } from 'src/models/Mica';
import { notifyError, notifySuccess } from 'src/utils/notify';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { MANDATORY_FIELDS } from 'src/composables/useDocumentModel';
import type { DocumentActionResult } from 'src/composables/useDocumentActions';
import { applyChosenFields, fromRestorable, toRestorable } from 'src/utils/restoreFields';
import type { ChosenField, DocumentDiff } from 'src/utils/restoreFields';

/**
 * The revisions of a draft document: the commits, a revision's content, the diff of two
 * revisions, and the restoration of a revision or of some of its fields.
 */
export function useDocumentHistory<T extends object>(target: MaybeRefOrGetter<DocumentTarget>) {
  const { locale } = useI18n({ useScope: 'global' });

  const commits = ref<GitCommitInfoDto[]>([]);
  const loading = ref(false);
  const busy = ref(false);

  function path() {
    return toValue(target).path;
  }

  async function fetchCommits(): Promise<GitCommitInfoDto[]> {
    loading.value = true;
    try {
      const response = await api.get<GitCommitInfoDto[]>(`${path()}/commits`);
      commits.value = response.data;
      return commits.value;
    } catch (error) {
      notifyError(error);
      commits.value = [];
      return [];
    } finally {
      loading.value = false;
    }
  }

  /** the document as it was at that commit */
  async function viewRevision(commitId: string): Promise<T> {
    const response = await api.get<T>(`${path()}/commit/${commitId}/view`);
    return response.data;
  }

  async function diff(leftCommitId: string, rightCommitId: string): Promise<DocumentDiff> {
    const response = await api.get<DocumentDiff>(`${path()}/_diff`, {
      params: { left: leftCommitId, right: rightCommitId, locale: locale.value },
    });
    return response.data;
  }

  async function run(operation: () => Promise<unknown>): Promise<DocumentActionResult> {
    busy.value = true;
    try {
      await operation();
      notifySuccess('history.restored');
      return 'updated';
    } catch (error) {
      notifyError(error);
      return 'failed';
    } finally {
      busy.value = false;
    }
  }

  /** the whole document goes back to that commit (a new commit) */
  function restore(commitId: string) {
    return run(() => api.put(`${path()}/commit/${commitId}/restore`));
  }

  /** the chosen fields of an older revision are applied to the current document (a new commit) */
  function restoreFields(chosen: ChosenField[]) {
    return run(async () => {
      const fields = MANDATORY_FIELDS[toValue(target).type].localized;
      const current = (await api.get<T>(path())).data;
      const restored = fromRestorable<T>(applyChosenFields(toRestorable(current, fields), chosen), fields);
      await api.put(path(), restored, { params: { comment: t('history.restored_fields_comment') } });
    });
  }

  return { commits, loading, busy, fetchCommits, viewRevision, diff, restore, restoreFields };
}
