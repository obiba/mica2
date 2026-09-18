<template>
  <q-table
    flat
    dense
    :rows="children"
    :columns="columns"
    row-key="path"
    selection="multiple"
    :selected="selected"
    :loading="loading"
    :pagination="{ rowsPerPage: 20 }"
    :rows-per-page-options="[20, 50, 100]"
    :no-data-label="t('files.empty')"
    @update:selected="emit('update:selected', [...$event] as FileDto[])"
  >
    <template v-slot:top-left>
      <q-btn v-if="!root" flat dense size="sm" icon="arrow_upward" :label="t('files.parent')" @click="emit('navigate-back')" />
    </template>
    <template v-slot:top-right>
      <span v-if="selected.length > 0" class="text-caption text-grey-7">
        {{ t('files.selected', { count: selected.length }) }}
        <q-btn flat dense size="sm" :label="t('files.clear_selection')" @click="emit('update:selected', [])" />
      </span>
    </template>
    <template v-slot:body-cell-name="props">
      <q-td :props="props" class="text-no-wrap">
        <q-icon :name="fileIcon(props.row)" class="q-mr-xs" />
        <a v-if="props.row.permissions?.view" href="#" class="text-primary" @click.prevent="emit('navigate', props.row)">{{ props.value }}</a>
        <span v-else>{{ props.value }}</span>
      </q-td>
    </template>
    <template v-slot:body-cell-status="props">
      <q-td :props="props" class="text-no-wrap">
        {{ t(`publish.status.${props.value}`) }}
        <q-icon v-if="isPublished(props.row)" name="star" color="warning" class="q-ml-xs">
          <q-tooltip>{{ t('publish.published') }} ({{ getDateLabel(props.row.state?.publicationDate) }} [{{ props.row.state?.publishedBy }}])</q-tooltip>
        </q-icon>
        <q-icon v-else name="star_outline" color="grey-6" class="q-ml-xs">
          <q-tooltip>{{ t('publish.not_published') }}</q-tooltip>
        </q-icon>
      </q-td>
    </template>
    <template v-slot:body-cell-actions="props">
      <q-td :props="props" class="text-no-wrap">
        <q-btn v-if="props.row.permissions?.view" flat dense round size="sm" icon="download" color="primary" type="a" :href="downloadUrl(props.row)" target="_self" :title="t('files.download')" />
        <q-btn v-if="canDelete(props.row)" flat dense round size="sm" icon="delete" color="negative" :title="t('delete')" @click="emit('delete', props.row)" />
      </q-td>
    </template>
  </q-table>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { FileDto } from 'src/models/Mica';
import { canDelete, fileIcon, isFolder, isPublished, sizeLabel } from 'src/utils/files';
import { getDateDistanceLabel, getDateLabel } from 'src/utils/dates';

interface Props {
  children: FileDto[];
  selected: FileDto[];
  downloadUrl: (file: FileDto) => string;
  root?: boolean;
  loading?: boolean;
}

defineProps<Props>();
const emit = defineEmits<{
  'update:selected': [files: FileDto[]];
  navigate: [file: FileDto];
  'navigate-back': [];
  delete: [file: FileDto];
}>();
const { t } = useI18n();

const columns = computed<QTableColumn[]>(() => [
  { name: 'name', label: t('name'), field: 'name', align: 'left', sortable: true },
  {
    name: 'lastUpdate',
    label: t('last_modified'),
    field: (row: FileDto) => row.timestamps?.lastUpdate,
    align: 'left',
    sortable: true,
    format: (value: string | undefined) => getDateDistanceLabel(value),
  },
  {
    name: 'size',
    label: t('files.size'),
    field: 'size',
    align: 'left',
    sortable: true,
    format: (value: number | undefined, row: FileDto) => (isFolder(row) ? t('files.items', { count: value ?? 0 }) : sizeLabel(value)),
  },
  { name: 'status', label: t('status'), field: 'revisionStatus', align: 'left', sortable: true },
  { name: 'actions', label: t('history.actions'), field: 'path', align: 'left' },
]);
</script>
