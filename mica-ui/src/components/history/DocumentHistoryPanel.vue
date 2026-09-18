<template>
  <div>
    <history-table
      :commits="commits"
      :selected="viewed"
      :published-id="state?.publishedId"
      :loading="loading"
      :can-restore="canRestore"
      @view="onView"
      @diff="onDiff"
      @restore="onRestoreRequest"
    />
    <revision-panel v-if="viewed" :commit="viewed" :loading="loadingRevision" class="q-mt-md">
      <slot name="revision" :document="revision" :model="revisionModel" :form-path="target.formPath">
        <entity-json-form v-if="revisionModel" :model-value="revisionModel" :form-path="target.formPath" readonly />
      </slot>
    </revision-panel>
    <diff-dialog
      v-model="showDiff"
      :left="diffLeft"
      :right="diffRight"
      :diff="diff"
      :loading="loadingDiff"
      :with-previous="diffWithPrevious"
      :can-restore="canRestore"
      @restore="onRestoreFields"
    />
    <confirm-dialog
      v-model="showRestore"
      :title="t('history.restore_title')"
      :text="t('history.restore_text', { date: toRestore ? getDateLabel(toRestore.date) : '' })"
      @confirm="onRestore"
    />
  </div>
</template>

<script setup lang="ts">
import type { EntityStateDto, GitCommitInfoDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import HistoryTable from 'src/components/history/HistoryTable.vue';
import RevisionPanel from 'src/components/history/RevisionPanel.vue';
import DiffDialog from 'src/components/history/DiffDialog.vue';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentHistory } from 'src/composables/useDocumentHistory';
import { useDocumentModel, type FormModel } from 'src/composables/useDocumentModel';
import type { ChosenField, DocumentDiff } from 'src/utils/restoreFields';
import { getDateLabel } from 'src/utils/dates';
import { notifyError } from 'src/utils/notify';

interface Props {
  target: DocumentTarget;
  state: EntityStateDto | undefined;
  /** the document is a draft the user can edit */
  canRestore?: boolean;
}

const props = defineProps<Props>();
/** a revision was restored: the document changed */
const emit = defineEmits<{ restored: [] }>();
const { t } = useI18n();

const { commits, loading, fetchCommits, viewRevision, diff: fetchDiff, restore, restoreFields } = useDocumentHistory<object>(
  () => props.target,
);
const { toModel } = useDocumentModel(props.target.type);

const viewed = ref<GitCommitInfoDto>();
const revision = ref<object>();
const revisionModel = ref<FormModel>();
const loadingRevision = ref(false);

const showDiff = ref(false);
const diffLeft = ref<GitCommitInfoDto>();
const diffRight = ref<GitCommitInfoDto>();
const diffWithPrevious = ref(false);
const diff = ref<DocumentDiff>();
const loadingDiff = ref(false);

const showRestore = ref(false);
const toRestore = ref<GitCommitInfoDto>();

async function load() {
  const list = await fetchCommits();
  if (list.length > 0) await onView(list[0] as GitCommitInfoDto);
}

async function onView(commit: GitCommitInfoDto) {
  viewed.value = commit;
  loadingRevision.value = true;
  try {
    revision.value = await viewRevision(commit.commitId);
    revisionModel.value = toModel(revision.value as { content?: string });
  } catch (error) {
    revision.value = undefined;
    revisionModel.value = undefined;
    notifyError(error);
  } finally {
    loadingRevision.value = false;
  }
}

async function onDiff(left: GitCommitInfoDto, right: GitCommitInfoDto, withPrevious: boolean) {
  diffLeft.value = left;
  diffRight.value = right;
  diffWithPrevious.value = withPrevious;
  diff.value = undefined;
  showDiff.value = true;
  loadingDiff.value = true;
  try {
    diff.value = await fetchDiff(left.commitId, right.commitId);
  } catch (error) {
    notifyError(error);
  } finally {
    loadingDiff.value = false;
  }
}

function onRestoreRequest(commit: GitCommitInfoDto) {
  toRestore.value = commit;
  showRestore.value = true;
}

async function onRestore() {
  if (!toRestore.value) return;
  if ((await restore(toRestore.value.commitId)) === 'updated') await onRestored();
}

/** from the diff dialog: some fields of the older revision, or the whole revision */
async function onRestoreFields(chosen: ChosenField[]) {
  if (!diffRight.value) return;
  const result = chosen.length > 0 ? await restoreFields(chosen) : await restore(diffRight.value.commitId);
  if (result === 'updated') await onRestored();
}

async function onRestored() {
  emit('restored');
  await load();
}

watch(() => props.target.id, load, { immediate: true });
</script>
