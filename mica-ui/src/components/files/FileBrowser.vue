<template>
  <div>
    <q-spinner-dots v-if="loading && !document" color="primary" size="2em" />
    <div v-else-if="document">
      <file-breadcrumbs :breadcrumbs="breadcrumbs" :icon="fileIcon(document)" class="q-mb-sm" @navigate="onNavigate" />
      <file-toolbar
        :document="document"
        :selection="selected"
        :download-url="downloadUrl(document)"
        :root="isRoot"
        :disable="busy"
        class="q-mb-md"
        @upload="upload"
        @add-folder="showAddFolder = true"
        @rename="showRename = true"
        @publish="publish"
        @status="toStatus"
        @delete="remove"
      />
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-8">
          <files-table
            v-if="!isCurrentFile"
            v-model:selected="selected"
            :children="children"
            :download-url="downloadUrl"
            :root="isRoot"
            :loading="loading || busy"
            @navigate="(file) => onNavigate(file.path)"
            @navigate-back="navigateBack"
            @delete="onDeleteChild"
          />
          <slot v-else name="file" :document="document" />
        </div>
        <div class="col-12 col-md-4">
          <file-detail-panel :document="document" />
        </div>
      </div>
      <name-dialog v-model="showAddFolder" :title="t('files.new_folder')" :label="t('name')" @submit="createFolder" />
      <name-dialog v-model="showRename" :title="t('files.rename')" :label="t('name')" :initial="document.name" @submit="(name) => document && rename(document, name)" />
      <confirm-dialog
        v-model="showDeleteChild"
        :title="t('files.delete_title')"
        :text="t('files.delete_text', { count: 1 })"
        @confirm="onDeleteChildConfirmed"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FileDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import FileBreadcrumbs from 'src/components/files/FileBreadcrumbs.vue';
import FileToolbar from 'src/components/files/FileToolbar.vue';
import FilesTable from 'src/components/files/FilesTable.vue';
import FileDetailPanel from 'src/components/files/FileDetailPanel.vue';
import NameDialog from 'src/components/files/NameDialog.vue';
import { useFileSystem } from 'src/composables/useFileSystem';
import { fileIcon, isUnder } from 'src/utils/files';

interface Props {
  /** the folder the browser is confined to: `/` or the folder of a document */
  root: string;
  /** the path to open, under the root (from the route) */
  path?: string | undefined;
}

const props = defineProps<Props>();
/** the browser moved to another path, for the page to reflect it in the route */
const emit = defineEmits<{ 'update:path': [path: string] }>();
const { t } = useI18n();

const {
  document,
  loading,
  busy,
  selected,
  path,
  children,
  isCurrentFile,
  isRoot,
  breadcrumbs,
  downloadUrl,
  navigateTo,
  navigateBack,
  createFolder,
  upload,
  rename,
  remove,
  publish,
  toStatus,
} = useFileSystem(() => props.root);

const showAddFolder = ref(false);
const showRename = ref(false);
const showDeleteChild = ref(false);
const childToDelete = ref<FileDto>();

function onNavigate(target: string) {
  navigateTo(target);
}

function onDeleteChild(file: FileDto) {
  childToDelete.value = file;
  showDeleteChild.value = true;
}

async function onDeleteChildConfirmed() {
  if (!childToDelete.value) return;
  selected.value = [childToDelete.value];
  await remove();
}

watch(path, (value) => {
  if (value !== props.path) emit('update:path', value);
});

watch(
  () => [props.root, props.path],
  () => {
    const target = props.path && isUnder(props.path, props.root) ? props.path : props.root;
    if (target !== path.value || !document.value) navigateTo(target);
  },
  { immediate: true },
);
</script>
