<template>
  <q-table
    flat
    dense
    :rows="commits"
    :columns="columns"
    row-key="commitId"
    :loading="loading"
    :filter="filter"
    :pagination="{ rowsPerPage: 10 }"
    :rows-per-page-options="[10, 25, 50]"
    :selected="selected ? [selected] : []"
    :no-data-label="t('history.none')"
  >
    <template v-slot:top-right>
      <q-input v-model="filter" dense debounce="300" :placeholder="t('search')">
        <template v-slot:append>
          <q-icon name="search" />
        </template>
      </q-input>
    </template>
    <template v-slot:body-cell-commitId="props">
      <q-td :props="props">
        <span :title="props.value" class="text-mono">{{ props.value.substring(0, 8) }}</span>
        <q-icon v-if="props.value === publishedId" name="star" color="warning" class="q-ml-xs">
          <q-tooltip>{{ t('publish.published') }}</q-tooltip>
        </q-icon>
      </q-td>
    </template>
    <template v-slot:body-cell-actions="props">
      <q-td :props="props" class="text-no-wrap">
        <q-btn flat dense size="sm" color="primary" :label="t('view')" @click="emit('view', props.row)" />
        <q-btn
          v-if="props.rowIndex < commits.length - 1"
          flat
          dense
          size="sm"
          color="primary"
          :label="t('history.diff_previous')"
          :title="t('history.diff_previous_tooltip')"
          @click="emit('diff', props.row, commits[props.rowIndex + 1] as GitCommitInfoDto, true)"
        />
        <q-btn
          v-if="props.rowIndex > 0"
          flat
          dense
          size="sm"
          color="primary"
          :label="t('history.diff_current')"
          :title="t('history.diff_current_tooltip')"
          @click="emit('diff', commits[0] as GitCommitInfoDto, props.row, false)"
        />
        <q-btn
          v-if="props.rowIndex > 0 && canRestore"
          flat
          dense
          size="sm"
          color="secondary"
          :label="t('history.restore')"
          @click="emit('restore', props.row)"
        />
      </q-td>
    </template>
  </q-table>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { GitCommitInfoDto } from 'src/models/Mica';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  /** newest first, as served */
  commits: GitCommitInfoDto[];
  /** the commit currently displayed */
  selected?: GitCommitInfoDto | undefined;
  /** the published commit, marked with a star */
  publishedId?: string | undefined;
  loading?: boolean;
  canRestore?: boolean;
}

defineProps<Props>();
const emit = defineEmits<{
  view: [commit: GitCommitInfoDto];
  /** `withPrevious`: the left commit is compared with the one before it (no restore) */
  diff: [left: GitCommitInfoDto, right: GitCommitInfoDto, withPrevious: boolean];
  restore: [commit: GitCommitInfoDto];
}>();
const { t } = useI18n();

const filter = ref('');

const columns = computed<QTableColumn[]>(() => [
  { name: 'commitId', label: 'ID', field: 'commitId', align: 'left' },
  { name: 'date', label: t('history.date'), field: 'date', align: 'left', format: (value: string) => getDateLabel(value) },
  { name: 'author', label: t('history.author'), field: 'author', align: 'left' },
  { name: 'comment', label: t('history.comment'), field: 'comment', align: 'left', style: 'width: 40%; white-space: normal' },
  { name: 'actions', label: t('history.actions'), field: 'commitId', align: 'left' },
]);
</script>
