import { defineStore } from 'pinia';
import type { FileDto } from 'src/models/Mica';
import { canEdit, canView } from 'src/utils/files';

export type ClipboardCommand = 'copy' | 'move';

/**
 * The file system clipboard: the files cut or copied, kept app-wide so that they can be pasted
 * after navigating to another folder, or another page.
 */
export const useFilesStore = defineStore('files', () => {
  const command = ref<ClipboardCommand>();
  /** the folder the items were taken from: pasting there is not allowed */
  const origin = ref<string>();
  const items = ref<FileDto[]>([]);

  const hasItems = computed(() => items.value.length > 0);

  function set(value: ClipboardCommand, from: string, files: FileDto[]) {
    command.value = files.length > 0 ? value : undefined;
    origin.value = files.length > 0 ? from : undefined;
    items.value = files;
  }

  /** copies the viewable files */
  function copy(from: string, files: FileDto[]) {
    set('copy', from, files.filter(canView));
  }

  /** cuts the editable (draft) files */
  function cut(from: string, files: FileDto[]) {
    set('move', from, files.filter(canEdit));
  }

  function clear() {
    set('copy', '', []);
  }

  return { command, origin, items, hasItems, copy, cut, clear };
});
