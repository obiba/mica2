<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('files.select_file') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <file-breadcrumbs :breadcrumbs="breadcrumbs" icon="folder" @navigate="open" />
      </q-card-section>
      <q-card-section style="height: 50vh" class="scroll q-pt-none">
        <q-list dense>
          <q-item v-if="!isRoot" clickable @click="open(parentPath(path))">
            <q-item-section avatar><q-icon name="arrow_upward" /></q-item-section>
            <q-item-section>{{ t('files.parent') }}</q-item-section>
          </q-item>
          <q-item
            v-for="child in children"
            :key="child.path"
            :clickable="isFolder(child) || isAccepted(child)"
            :disable="!isFolder(child) && !isAccepted(child)"
            :active="child.path === selected"
            @click="onClick(child)"
            @dblclick="isAccepted(child) && onSelect()"
          >
            <q-item-section avatar><q-icon :name="fileIcon(child)" /></q-item-section>
            <q-item-section>{{ child.name }}</q-item-section>
          </q-item>
          <q-item v-if="!loading && children.length === 0">
            <q-item-section class="text-hint">{{ t('files.empty') }}</q-item-section>
          </q-item>
        </q-list>
        <q-inner-loading :showing="loading" />
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
import type { FileDto } from 'src/models/Mica';
import FileBreadcrumbs from 'src/components/files/FileBreadcrumbs.vue';
import { useFileSystem } from 'src/composables/useFileSystem';
import { fileIcon, hasExtension, isFile, isFolder, parentPath } from 'src/utils/files';

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
const { children, loading, isRoot, breadcrumbs, path, navigateTo } = useFileSystem('/');
/** the path of the selected file */
const selected = ref<string>();

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
