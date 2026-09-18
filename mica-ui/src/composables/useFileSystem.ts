import type { MaybeRefOrGetter } from 'vue';
import { api, toServerUrl } from 'src/boot/api';
import type { AttachmentDto, FileDto } from 'src/models/Mica';
import { notifyError } from 'src/utils/notify';
import { useTempFiles } from 'src/composables/useTempFiles';
import type { FileStatus } from 'src/utils/files';
import { breadcrumbsOf, canDelete, canPublish, canUnpublish, canGoTo, isFile, isUnder, joinPath, parentPath } from 'src/utils/files';

/**
 * The draft file system under a root folder (`/` for the whole system, `/network/{id}` for the
 * files of a document): browse, upload, create folders, rename, delete, publish and change the
 * status of the files, one or several at a time.
 */
export function useFileSystem(root: MaybeRefOrGetter<string>) {
  const tempFiles = useTempFiles();

  const document = ref<FileDto>();
  const loading = ref(false);
  const busy = ref(false);
  /** the children selected for a multi-file operation */
  const selected = ref<FileDto[]>([]);

  const path = computed(() => document.value?.path ?? toValue(root));
  const children = computed(() => document.value?.children ?? []);
  const isCurrentFile = computed(() => isFile(document.value));
  const isRoot = computed(() => path.value === toValue(root));
  const breadcrumbs = computed(() => breadcrumbsOf(path.value, toValue(root)));

  function encodePath(value: string): string {
    return value.split('/').map(encodeURIComponent).join('/');
  }

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

  function refresh() {
    return navigateTo(path.value);
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
        const existing = children.value.find((child) => child.type === 'FILE' && child.name === file.name)?.state?.attachment;
        const attachment: Partial<AttachmentDto> = existing
          ? { ...existing, timestamps: undefined }
          : { fileName: uploaded.fileName, path: path.value };
        await addAttachment({ ...attachment, id: uploaded.id, size: uploaded.size, md5: uploaded.md5, justUploaded: true });
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
      (failure) => files.length === 1 || ((failure.reason as { response?: { status?: number } })?.response?.status ?? 500) >= 500,
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
    return run(
      () => applyToFiles(files, (file) => api.put(fileUrl(file.path), null, { params: { status } })),
      refresh,
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
    downloadUrl,
    navigateTo,
    navigateBack,
    refresh,
    createFolder,
    upload,
    rename,
    remove,
    publish,
    toStatus,
  };
}
