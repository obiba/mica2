<template>
  <div class="row items-center q-gutter-sm">
    <template v-if="!file">
      <q-btn
        color="primary"
        icon="upload"
        :label="t('files.upload')"
        size="sm"
        :disable="!editable || disable"
        @click="input?.click()"
      />
      <input ref="input" type="file" multiple class="hidden" @change="onFilesSelected" />
      <q-btn
        color="primary"
        outline
        icon="create_new_folder"
        :label="t('files.new_folder')"
        size="sm"
        :disable="!editable || disable"
        @click="emit('add-folder')"
      />
    </template>
    <q-btn
      v-if="document.permissions?.view"
      color="primary"
      outline
      icon="download"
      :label="t('files.download')"
      size="sm"
      type="a"
      :href="downloadUrl"
      target="_self"
    />
    <q-btn
      v-if="!root"
      flat
      icon="drive_file_rename_outline"
      :label="t('files.rename')"
      size="sm"
      :disable="!editable || disable"
      @click="emit('rename')"
    />
    <q-btn-group flat>
      <q-btn
        flat
        icon="content_copy"
        size="sm"
        :title="t('files.copy')"
        :disable="!copyable || disable"
        @click="emit('copy')"
      />
      <q-btn
        flat
        icon="content_cut"
        size="sm"
        :title="t('files.cut')"
        :disable="!cuttable || disable"
        @click="emit('cut')"
      />
      <q-btn
        flat
        icon="content_paste"
        size="sm"
        :title="pasteTitle"
        :disable="!canPaste || disable"
        @click="emit('paste')"
      >
        <q-badge v-if="clipboard.hasItems" color="primary" floating>{{ clipboard.items.length }}</q-badge>
      </q-btn>
    </q-btn-group>
    <q-space />
    <file-status-buttons
      :document="document"
      :selection="selection"
      :disable="disable"
      @publish="emit('publish', $event)"
      @status="emit('status', $event)"
      @delete="emit('delete')"
    />
  </div>
</template>

<script setup lang="ts">
import type { FileDto } from 'src/models/Mica';
import FileStatusButtons from 'src/components/files/FileStatusButtons.vue';
import type { FileStatus } from 'src/utils/files';
import { useFilesStore } from 'src/stores/files';
import { canEdit, canView, isFile } from 'src/utils/files';

interface Props {
  document: FileDto;
  selection: FileDto[];
  downloadUrl: string;
  /** the document is the root of the browser: it cannot be renamed, copied or moved */
  root?: boolean;
  /** the clipboard can be pasted into the document */
  canPaste?: boolean;
  disable?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  upload: [files: File[]];
  'add-folder': [];
  rename: [];
  publish: [value: boolean];
  status: [status: FileStatus];
  delete: [];
  copy: [];
  cut: [];
  paste: [];
}>();
const { t } = useI18n();
const clipboard = useFilesStore();

const input = ref<HTMLInputElement>();
const file = computed(() => isFile(props.document));
const editable = computed(() => canEdit(props.document));
/** the files a clipboard command applies to: the selection, or the document when not the root */
const clipboardTargets = computed(() =>
  props.selection.length > 0 ? props.selection : props.root ? [] : [props.document],
);
const copyable = computed(() => clipboardTargets.value.some(canView));
const cuttable = computed(() => clipboardTargets.value.some(canEdit));
const pasteTitle = computed(() =>
  clipboard.hasItems ? t(`files.paste_${clipboard.command}`, { count: clipboard.items.length }) : t('files.paste'),
);

function onFilesSelected(event: Event) {
  const target = event.target as HTMLInputElement;
  const files = Array.from(target.files ?? []);
  target.value = '';
  if (files.length > 0) emit('upload', files);
}
</script>
