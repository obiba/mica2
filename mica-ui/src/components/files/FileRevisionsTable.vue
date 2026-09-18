<template>
  <div>
    <div class="text-subtitle2 q-mb-sm">{{ t('files.revisions') }}</div>
    <q-table
      flat
      dense
      :rows="revisions"
      :columns="columns"
      row-key="id"
      :pagination="{ rowsPerPage: 20 }"
      :rows-per-page-options="[20, 50, 100]"
      :no-data-label="t('files.no_revisions')"
    >
      <template v-slot:body-cell-id="props">
        <q-td :props="props" class="text-no-wrap">
          <span class="text-caption" :title="props.value">{{ props.value }}</span>
          <q-icon v-if="props.value === document.state?.publishedId" name="star" color="warning" class="q-ml-xs">
            <q-tooltip
              >{{ t('publish.published') }} ({{ getDateLabel(document.state?.publicationDate) }} [{{
                document.state?.publishedBy
              }}])</q-tooltip
            >
          </q-icon>
          <q-badge v-if="props.value === document.state?.attachment?.id" color="grey-7" class="q-ml-xs">{{
            t('files.current')
          }}</q-badge>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props" class="text-no-wrap">
          <q-btn
            v-if="canRestore(props.row)"
            flat
            dense
            round
            size="sm"
            icon="undo"
            color="primary"
            :title="t('files.restore')"
            :disable="disable"
            @click="onRestoreRequest(props.row)"
          />
          <q-btn
            flat
            dense
            round
            size="sm"
            icon="download"
            color="primary"
            type="a"
            :href="downloadUrl(document, props.row.id)"
            target="_self"
            :title="t('files.download')"
          />
        </q-td>
      </template>
    </q-table>
    <confirm-dialog
      v-model="showRestore"
      :title="t('files.restore_title')"
      :text="t('files.restore_text', { date: restoreDate })"
      @confirm="onRestore"
    />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { AttachmentDto, FileDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import { canEdit } from 'src/utils/files';
import { getDateDistanceLabel, getDateLabel } from 'src/utils/dates';

interface Props {
  /** the file whose revisions are listed */
  document: FileDto;
  downloadUrl: (file: FileDto, version: string) => string;
  disable?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ restore: [revision: AttachmentDto] }>();
const { t } = useI18n();

const showRestore = ref(false);
const toRestore = ref<AttachmentDto>();

/** most recent first */
const revisions = computed(() => [...(props.document.state?.revisions ?? [])].reverse());
const restoreDate = computed(() => getDateLabel(toRestore.value?.timestamps?.created));

/** a previous revision of a draft file can be made the current one */
function canRestore(revision: AttachmentDto): boolean {
  return canEdit(props.document) && revision.id !== props.document.state?.attachment?.id;
}

function onRestoreRequest(revision: AttachmentDto) {
  toRestore.value = revision;
  showRestore.value = true;
}

function onRestore() {
  if (toRestore.value) emit('restore', toRestore.value);
}

const columns = computed<QTableColumn[]>(() => [
  { name: 'id', label: t('files.revision'), field: 'id', align: 'left' },
  {
    name: 'created',
    label: t('created'),
    field: (row: AttachmentDto) => row.timestamps?.created,
    align: 'left',
    sortable: true,
    format: (value: string | undefined) => getDateLabel(value),
  },
  {
    name: 'lastUpdate',
    label: t('last_modified'),
    field: (row: AttachmentDto) => row.timestamps?.lastUpdate,
    align: 'left',
    sortable: true,
    format: (value: string | undefined) => getDateDistanceLabel(value),
  },
  { name: 'actions', label: t('history.actions'), field: 'id', align: 'left' },
]);
</script>
