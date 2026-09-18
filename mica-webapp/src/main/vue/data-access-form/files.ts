import type { FileItem, FileUploadHooks } from '@obiba/quasar-ui-json-form';

/**
 * Mica temp-file flow for the `obibaFiles` controls: upload to the temp store, read the file
 * metadata, delete just-uploaded files on removal; stored files (with a `path`, set by the server
 * on save) are downloaded from the entity's form attachments endpoint.
 */
export function createFileUploadHooks(contextPath: string): FileUploadHooks {
  const ws = (path: string) => `${contextPath}/ws${path}`;

  return {
    async upload(file: File): Promise<FileItem> {
      const body = new FormData();
      body.append('file', file, file.name);
      const upload = await fetch(ws('/files/temp'), { method: 'POST', body });
      if (!upload.ok) {
        throw new Error(`Upload failed with status ${upload.status}`);
      }
      const location = upload.headers.get('Location') || '';
      const id = location.substring(location.lastIndexOf('/') + 1);
      const metadata = await fetch(ws(`/files/temp/${id}`), { headers: { Accept: 'application/json' } });
      if (!metadata.ok) {
        throw new Error(`Cannot read uploaded file with status ${metadata.status}`);
      }
      const tempFile = (await metadata.json()) as FileItem;
      return { fileName: file.name, size: file.size, ...tempFile, justUploaded: true };
    },

    async remove(item: FileItem): Promise<void> {
      // stored files are deleted by the server on save
      if (item.justUploaded && item.id) {
        await fetch(ws(`/files/temp/${item.id}`), { method: 'DELETE' });
      }
    },

    downloadUrl(item: FileItem): string | undefined {
      // just-uploaded files have no path yet and cannot be downloaded (same as the ASF widget)
      if (!item.path || !item.id) return undefined;
      return ws(`${item.path}/form/attachments/${encodeURIComponent(String(item.fileName))}/${item.id}/_download`);
    },
  };
}
