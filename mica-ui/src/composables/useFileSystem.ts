import type { MaybeRefOrGetter } from 'vue';
import { api, toServerUrl } from 'src/boot/api';
import type { AttachmentDto, FileDto } from 'src/models/Mica';
import { notifyError, notifySuccess, notifyWarning } from 'src/utils/notify';
import { useTempFiles } from 'src/composables/useTempFiles';
import { useFilesStore } from 'src/stores/files';
import type { FileStatus } from 'src/utils/files';
import {
  breadcrumbsOf,
  canDelete,
  canEdit as canEditFile,
  canPublish,
  canUnpublish,
  canGoTo,
  encodePath,
  isFile,
  isFolder,
  isUnder,
  joinPath,
  parentPath,
} from 'src/utils/files';

/** the predefined searches */
export type FileSearchShortcut = 'NOT_PUBLISHED' | 'UNDER_REVIEW' | 'DELETED' | 'RECENT';
export const FILE_SEARCH_SHORTCUTS: FileSearchShortcut[] = ['NOT_PUBLISHED', 'UNDER_REVIEW', 'DELETED', 'RECENT'];

export interface FileSearch {
  /** the text typed by the user, or the shortcut */
  query: string;
  recursively: boolean;
  shortcut?: FileSearchShortcut | undefined;
}

/** the server side query and options of a search or a shortcut */
function searchParams(search: FileSearch): Record<string, string | number | boolean> {
  const params: Record<string, string | number | boolean> = { recursively: search.recursively, limit: 999 };
  switch (search.shortcut) {
    case 'DELETED':
    case 'UNDER_REVIEW':
      params.query = `revisionStatus:${search.shortcut}`;
      break;
    case 'NOT_PUBLISHED':
      params.query = 'NOT(publicationDate:*)';
      break;
    case 'RECENT':
      params.query = '';
      params.sort = 'lastModifiedDate';
      params.order = 'desc';
      params.limit = 10;
      break;
    default:
      params.query = search.query;
  }
  return params;
}

/**
 * The draft file system under a root folder (`/` for the whole system, `/network/{id}` for the
 * files of a document): browse, search, upload, create folders, rename, copy, move, delete,
 * publish and change the status of the files, one or several at a time; restore a revision and
 * edit the details of a file.
 */
export function useFileSystem(root: MaybeRefOrGetter<string>) {
  const tempFiles = useTempFiles();
  const clipboard = useFilesStore();

  const document = ref<FileDto>();
  const loading = ref(false);
  const busy = ref(false);
  /** the children selected for a multi-file operation */
  const selected = ref<FileDto[]>([]);
  /** the search in progress, its results replacing the children of the current folder */
  const search = ref<FileSearch>();

  const path = computed(() => document.value?.path ?? toValue(root));
  const children = computed(() => document.value?.children ?? []);
  const isCurrentFile = computed(() => isFile(document.value));
  const isRoot = computed(() => path.value === toValue(root));
  const breadcrumbs = computed(() => breadcrumbsOf(path.value, toValue(root)));
  const searching = computed(() => search.value !== undefined);
  /** the files that can be pasted here: a folder, editable, other than the one they were taken from */
  const canPaste = computed(
    () =>
      clipboard.hasItems &&
      document.value !== undefined &&
      isFolder(document.value) &&
      canEditFile(document.value) &&
      clipboard.origin !== path.value,
  );

  function fileUrl(value: string): string {
    return `/draft/file${encodePath(value)}`;
  }

  function downloadUrl(target: FileDto, version?: string): string {
    return toServerUrl(`/draft/file-dl${encodePath(target.path)}${version ? `?version=${version}` : ''}`);
  }

  async function navigateTo(target: string): Promise<void> {
    const destination = isUnder(target, toValue(root)) ? target : toValue(root);
    loading.value = true;
    try {
      const response = await api.get<FileDto>(fileUrl(destination));
      document.value = { ...response.data, children: response.data.children ?? [] };
      selected.value = [];
      search.value = undefined;
    } catch (error) {
      notifyError(error);
      // a document that cannot be reached any more: back to its parent, or to the root
      if (destination !== toValue(root) && (error as { response?: { status?: number } })?.response?.status !== 403) {
        await navigateTo(parentPath(destination));
      } else if (!document.value) {
        document.value = undefined;
      }
    } finally {
      loading.value = false;
    }
  }

  /** reloads the current document, or reruns the search in progress */
  function refresh() {
    return search.value ? runSearch(search.value) : navigateTo(path.value);
  }

  async function runSearch(value: FileSearch): Promise<void> {
    loading.value = true;
    try {
      const response = await api.get<FileDto[]>(`/draft/files-search${encodePath(path.value)}`, {
        params: searchParams(value),
      });
      if (document.value) document.value = { ...document.value, children: response.data };
      selected.value = [];
      search.value = value;
    } catch (error) {
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  /** searches the files of the current folder by name, recursively or not */
  function searchFiles(query: string, recursively = search.value?.recursively ?? false) {
    return query.trim() ? runSearch({ query: query.trim(), recursively }) : clearSearch();
  }

  function searchShortcut(shortcut: FileSearchShortcut, recursively = search.value?.recursively ?? true) {
    return runSearch({ query: shortcut, recursively, shortcut });
  }

  function clearSearch() {
    return search.value ? navigateTo(path.value) : Promise.resolve();
  }

  function navigateBack() {
    return isRoot.value ? Promise.resolve() : navigateTo(parentPath(path.value));
  }

  async function run<R>(operation: () => Promise<R>, then?: () => Promise<void>): Promise<R | undefined> {
    busy.value = true;
    try {
      const result = await operation();
      if (then) await then();
      return result;
    } catch (error) {
      notifyError(error);
      await refresh();
      return undefined;
    } finally {
      busy.value = false;
    }
  }

  /** an attachment added or updated in the file system (`POST /draft/files`) */
  function addAttachment(attachment: Partial<AttachmentDto>) {
    return api.post('/draft/files', attachment);
  }

  function createFolder(name: string) {
    const folderPath = joinPath(path.value, name.trim());
    return run(
      () => addAttachment({ id: '', path: folderPath, fileName: '.' }),
      () => navigateTo(folderPath),
    );
  }

  /** uploads the files into the current folder; a file with the same name gets a new revision */
  function upload(files: File[]) {
    return run(async () => {
      for (const file of files) {
        const uploaded = await tempFiles.upload(file);
        const existing = children.value.find((child) => child.type === 'FILE' && child.name === file.name)?.state
          ?.attachment;
        const attachment: Partial<AttachmentDto> = existing
          ? { ...existing, timestamps: undefined }
          : { fileName: uploaded.fileName, path: path.value };
        await addAttachment({
          ...attachment,
          id: uploaded.id,
          size: uploaded.size,
          md5: uploaded.md5,
          justUploaded: true,
        });
      }
    }, refresh);
  }

  function rename(target: FileDto, name: string) {
    const newPath = joinPath(parentPath(target.path), name.trim());
    return run(
      () => api.put(fileUrl(target.path), null, { params: { name: name.trim() } }),
      () => navigateTo(target.path === path.value ? newPath : path.value),
    );
  }

  /** the files the operation applies to: the selection, or the current document */
  function targets(filter: (file: FileDto) => boolean): FileDto[] {
    const files = selected.value.length > 0 ? selected.value : document.value ? [document.value] : [];
    return files.filter(filter);
  }

  /** applies an operation to several files, ignoring the conflicts (4xx) when there are several */
  async function applyToFiles(files: FileDto[], operation: (file: FileDto) => Promise<unknown>): Promise<void> {
    const results = await Promise.allSettled(files.map(operation));
    const failures = results.filter((result): result is PromiseRejectedResult => result.status === 'rejected');
    const blocking = failures.filter(
      (failure) =>
        files.length === 1 || ((failure.reason as { response?: { status?: number } })?.response?.status ?? 500) >= 500,
    );
    if (blocking.length > 0) throw blocking[0]?.reason;
  }

  /** deletes the current document, or the selected files, that are in the DELETED status */
  function remove() {
    const files = targets(canDelete);
    const currentDeleted = selected.value.length === 0;
    return run(
      () => applyToFiles(files, (file) => api.delete(fileUrl(file.path))),
      () => (currentDeleted ? navigateTo(parentPath(path.value)) : refresh()),
    );
  }

  function publish(value: boolean) {
    const files = targets(value ? canPublish : canUnpublish);
    return run(
      () => applyToFiles(files, (file) => api.put(fileUrl(file.path), null, { params: { publish: value } })),
      refresh,
    );
  }

  function toStatus(status: FileStatus) {
    const files = targets((file) => canGoTo(file, status));
    return run(() => applyToFiles(files, (file) => api.put(fileUrl(file.path), null, { params: { status } })), refresh);
  }

  /** puts the selection, or the current document, in the clipboard */
  function copyToClipboard() {
    clipboard.copy(
      path.value,
      targets(() => true),
    );
  }

  function cutToClipboard() {
    clipboard.cut(
      path.value,
      targets(() => true),
    );
  }

  /** copies or moves the clipboard files into the current folder; a folder is pasted as a subfolder */
  function paste() {
    if (clipboard.hasItems && clipboard.origin === path.value) {
      notifyWarning('files.invalid_paste');
      return Promise.resolve(undefined);
    }
    if (!canPaste.value) return Promise.resolve(undefined);
    const command = clipboard.command;
    const destination = path.value;
    const items = [...clipboard.items];
    return run(
      () =>
        applyToFiles(items, (file) =>
          api.put(fileUrl(file.path), null, {
            params: { [command as string]: isFolder(file) ? joinPath(destination, file.name) : destination },
          }),
        ),
      async () => {
        clipboard.clear();
        await refresh();
      },
    );
  }

  /** makes a previous revision of the current file the draft one */
  function restoreRevision(revision: AttachmentDto) {
    return run(() => api.put(fileUrl(path.value), null, { params: { version: revision.id } }), refresh);
  }

  /** saves the type and description of the current file, as a new revision */
  function updateDetails(details: Pick<AttachmentDto, 'type' | 'description'>) {
    const attachment = document.value?.state?.attachment;
    if (!attachment) return Promise.resolve(undefined);
    // the size is not serialized for an attachment without md5: keep the file's one
    const size = attachment.size ?? document.value?.size;
    return run(
      () => addAttachment({ ...attachment, ...details, size, timestamps: undefined }),
      async () => {
        notifySuccess('files.details_saved');
        await refresh();
      },
    );
  }

  return {
    document,
    loading,
    busy,
    selected,
    path,
    children,
    isCurrentFile,
    isRoot,
    breadcrumbs,
    search,
    searching,
    canPaste,
    downloadUrl,
    navigateTo,
    navigateBack,
    refresh,
    searchFiles,
    searchShortcut,
    clearSearch,
    createFolder,
    upload,
    rename,
    remove,
    publish,
    toStatus,
    copyToClipboard,
    cutToClipboard,
    paste,
    restoreRevision,
    updateDetails,
  };
}
