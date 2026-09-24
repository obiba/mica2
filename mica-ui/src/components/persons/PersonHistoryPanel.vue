<template>
  <div>
    <history-table :commits="commits" :selected="viewed" :loading="loading" @view="onView" @diff="onDiff" />
    <revision-panel v-if="viewed" :commit="viewed" :loading="loadingRevision" class="q-mt-md">
      <person-form v-if="revision" :model-value="toPersonModel(revision)" readonly />
    </revision-panel>
    <diff-dialog
      v-model="showDiff"
      :left="diffLeft"
      :right="diffRight"
      :diff="diff"
      :loading="loadingDiff"
      :with-previous="diffWithPrevious"
    />
  </div>
</template>

<script setup lang="ts">
import type { GitCommitInfoDto, PersonDto } from 'src/models/Mica';
import HistoryTable from 'src/components/history/HistoryTable.vue';
import RevisionPanel from 'src/components/history/RevisionPanel.vue';
import DiffDialog from 'src/components/history/DiffDialog.vue';
import PersonForm from 'src/components/persons/PersonForm.vue';
import { useDocumentHistory } from 'src/composables/useDocumentHistory';
import type { DocumentDiff } from 'src/utils/restoreFields';
import { notifyError } from 'src/utils/notify';
import { toPersonModel } from 'src/utils/persons';

interface Props {
  id: string;
}

const props = defineProps<Props>();

// as the legacy admin app: the revisions can be viewed and compared, not restored
const {
  commits,
  loading,
  fetchCommits,
  viewRevision,
  diff: fetchDiff,
} = useDocumentHistory<PersonDto>(() => ({
  path: `/draft/person/${props.id}`,
}));

const viewed = ref<GitCommitInfoDto>();
const revision = ref<PersonDto>();
const loadingRevision = ref(false);

const showDiff = ref(false);
const diffLeft = ref<GitCommitInfoDto>();
const diffRight = ref<GitCommitInfoDto>();
const diffWithPrevious = ref(false);
const diff = ref<DocumentDiff>();
const loadingDiff = ref(false);

async function load() {
  const list = await fetchCommits();
  if (list.length > 0) await onView(list[0] as GitCommitInfoDto);
}

async function onView(commit: GitCommitInfoDto) {
  viewed.value = commit;
  loadingRevision.value = true;
  try {
    revision.value = await viewRevision(commit.commitId);
  } catch (error) {
    revision.value = undefined;
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

watch(() => props.id, load, { immediate: true });
</script>
