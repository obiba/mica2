<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ t('files.select_file') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <file-breadcrumbs :breadcrumbs="breadcrumbs" icon="folder" @navigate="open" />
      </q-card-section>
      <q-card-section style="height: 50vh" class="scroll q-pt-none">
        <q-table
          flat
          dense
          :rows="children"
          :columns="columns"
          row-key="path"
          :loading="loading"
          :pagination="{ rowsPerPage: 0 }"
          hide-bottom
          :no-data-label="t('files.empty')"
        >
          <template v-slot:body="props">
            <q-tr
              :props="props"
              :class="{
                'cursor-pointer': isFolder(props.row) || isAccepted(props.row),
                'text-grey-5': !isFolder(props.row) && !isAccepted(props.row),
                'bg-blue-1': props.row.path === selected,
              }"
              @click="onClick(props.row)"
              @dblclick="isAccepted(props.row) && onSelect()"
            >
              <q-td v-for="col in props.cols" :key="col.name" :props="props" class="text-no-wrap">
                <template v-if="col.name === 'name'">
                  <q-icon :name="fileIcon(props.row)" class="q-mr-xs" />
                  <span :class="{ 'text-primary': isFolder(props.row) || isAccepted(props.row) }">{{ col.value }}</span>
                </template>
                <template v-else-if="col.name === 'published'">
                  <template v-if="isFolder(props.row)" />
                  <q-icon v-else-if="isPublished(props.row)" name="star" color="warning">
                    <q-tooltip
                      >{{ t('publish.published') }} ({{ getDateLabel(props.row.state?.publicationDate) }})</q-tooltip
                    >
                  </q-icon>
                  <q-icon v-else name="star_outline" color="grey-6">
                    <q-tooltip>{{ t('publish.not_published') }}</q-tooltip>
                  </q-icon>
                </template>
                <template v-else>{{ col.value }}</template>
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn :label="t('files.select')" color="primary" :disable="!selected" @click="onSelect" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { FileDto } from 'src/models/Mica';
import FileBreadcrumbs from 'src/components/files/FileBreadcrumbs.vue';
import { useFileSystem } from 'src/composables/useFileSystem';
import { fileIcon, hasExtension, isFile, isFolder, isPublished, sizeLabel } from 'src/utils/files';
import { getDateDistanceLabel, getDateLabel } from 'src/utils/dates';

interface Props {
  /** the folder to open */
  folder?: string | undefined;
  /** the extensions of the files that can be selected (e.g. `.xlsx`), any when empty */
  extensions?: string[];
}

const props = withDefaults(defineProps<Props>(), { folder: '/', extensions: () => [] });
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ select: [path: string] }>();
const { t } = useI18n();

// the draft file system: the file sources are not meant to be published
const { children, loading, breadcrumbs, navigateTo } = useFileSystem('/');
/** the path of the selected file */
const selected = ref<string>();

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
    format: (value: number | undefined, row: FileDto) =>
      isFolder(row) ? t('files.items', { count: value ?? 0 }) : sizeLabel(value),
  },
  { name: 'published', label: t('publish.published'), field: (row: FileDto) => isPublished(row), align: 'center' },
]);

function isAccepted(file: FileDto) {
  return isFile(file) && hasExtension(file.name, props.extensions);
}

/** the selection is of the folder shown */
function open(folder: string) {
  selected.value = undefined;
  navigateTo(folder);
}

function onShow() {
  open(props.folder);
}

function onClick(file: FileDto) {
  if (isFolder(file)) open(file.path);
  else selected.value = file.path;
}

function onSelect() {
  if (!selected.value) return;
  emit('select', selected.value);
  show.value = false;
}
</script>
