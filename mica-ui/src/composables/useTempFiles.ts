import { api, toServerUrl } from 'src/boot/api';
import type { AttachmentDto, TempFileDto } from 'src/models/Mica';

/**
 * The temporary file store (`/files/temp`): a file is uploaded there first, then referenced as a
 * `justUploaded` attachment in the document, which the server moves on save.
 */
export function useTempFiles() {
  async function upload(file: File, onProgress?: (percent: number) => void): Promise<AttachmentDto> {
    const body = new FormData();
    body.append('file', file, file.name);
    const response = await api.post('/files/temp', body, {
      onUploadProgress: (event) => {
        if (onProgress && event.total) onProgress(Math.round((100 * event.loaded) / event.total));
      },
    });
    const location: string = response.headers['location'] ?? '';
    const id = location.substring(location.lastIndexOf('/') + 1);
    if (!id) throw new Error(`Upload response has no file location (Location: '${location}')`);
    const metadata = await api.get<TempFileDto>(`/files/temp/${id}`);
    const tempFile = metadata.data;
    return {
      id: tempFile.id,
      fileName: tempFile.name || file.name,
      size: tempFile.size ?? file.size,
      md5: tempFile.md5,
      justUploaded: true,
      lang: 'en',
      description: [],
      attributes: [],
      timestamps: { created: new Date().toISOString() },
    };
  }

  /** deletes a just-uploaded file from the temporary store (stored attachments are handled by the server) */
  async function remove(attachment: AttachmentDto): Promise<void> {
    if (attachment.justUploaded && attachment.id) {
      await api.delete(`/files/temp/${attachment.id}`);
    }
  }

  /** where a document attachment can be downloaded: the temp store until the document is saved */
  function downloadUrl(attachment: AttachmentDto, documentPath: string): string {
    return attachment.justUploaded
      ? toServerUrl(`/files/temp/${attachment.id}/_download`)
      : toServerUrl(`${documentPath}/file/${attachment.id}/_download`);
  }

  return { upload, remove, downloadUrl };
}
