import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import type { FileDto } from 'src/models/Mica';
import { FileType } from 'src/models/Mica';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), put: vi.fn(), post: vi.fn(), delete: vi.fn() },
  toServerUrl: (p: string) => `/ws${p}`,
}));
vi.mock('src/utils/notify', () => ({ notifyError: vi.fn(), notifySuccess: vi.fn(), notifyWarning: vi.fn() }));
vi.mock('src/composables/useTempFiles', () => ({
  useTempFiles: () => ({
    upload: vi.fn(async (file: File) => ({ id: 'tmp1', fileName: file.name, size: 3, md5: 'x' })),
  }),
}));

import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import { useFileSystem } from './useFileSystem';
import { useFilesStore } from 'src/stores/files';

const mocked = api as unknown as Record<'get' | 'put' | 'post' | 'delete', ReturnType<typeof vi.fn>>;

function folder(path: string, children: FileDto[] = []): FileDto {
  return {
    name: path.substring(path.lastIndexOf('/') + 1),
    path,
    type: FileType.FOLDER,
    children,
    description: [],
    revisionStatus: 'DRAFT',
    permissions: { view: true, edit: true, delete: true, publish: true },
  };
}

function file(path: string, overrides: Partial<FileDto> = {}): FileDto {
  const name = path.substring(path.lastIndexOf('/') + 1);
  return {
    name,
    path,
    type: FileType.FILE,
    children: [],
    description: [],
    revisionStatus: 'DRAFT',
    permissions: { view: true, edit: true, delete: true, publish: true },
    state: {
      id: 's',
      name,
      path,
      revisions: [],
      attachment: {
        id: 'v2',
        fileName: name,
        path: path.substring(0, path.lastIndexOf('/')),
        description: [],
        attributes: [],
      },
    },
    ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  setActivePinia(createPinia());
  mocked.get.mockImplementation(async (url: string) => ({ data: folder(url.replace('/draft/file', '')) }));
  mocked.put.mockResolvedValue({});
  mocked.post.mockResolvedValue({});
  mocked.delete.mockResolvedValue({});
});

describe('useFileSystem', () => {
  it('browses under the root only', async () => {
    const fs = useFileSystem('/network/n');
    await fs.navigateTo('/network/n/docs');
    expect(mocked.get).toHaveBeenCalledWith('/draft/file/network/n/docs');
    expect(fs.breadcrumbs.value.map((c) => c.name)).toEqual(['n', 'docs']);
    expect(fs.isRoot.value).toBe(false);
    await fs.navigateBack();
    expect(fs.path.value).toBe('/network/n');
    expect(fs.isRoot.value).toBe(true);
    await fs.navigateTo('/elsewhere');
    expect(fs.path.value).toBe('/network/n');
    expect(fs.downloadUrl(file('/network/n/a b.pdf'), 'v1')).toBe('/ws/draft/file-dl/network/n/a%20b.pdf?version=v1');
  });

  it('falls back to the parent when a document is gone', async () => {
    mocked.get.mockImplementationOnce(async () => {
      throw { response: { status: 404 } };
    });
    const fs = useFileSystem('/');
    await fs.navigateTo('/network/gone');
    expect(notifyError).toHaveBeenCalled();
    expect(fs.path.value).toBe('/network');
  });

  it('creates a folder and navigates into it', async () => {
    const fs = useFileSystem('/');
    await fs.navigateTo('/network/n');
    await fs.createFolder(' docs ');
    expect(mocked.post).toHaveBeenCalledWith('/draft/files', { id: '', path: '/network/n/docs', fileName: '.' });
    expect(fs.path.value).toBe('/network/n/docs');
  });

  it('uploads files, as new revisions of the existing ones', async () => {
    const existing = file('/network/n/a.pdf');
    mocked.get.mockImplementation(async () => ({ data: folder('/network/n', [existing]) }));
    const fs = useFileSystem('/network/n');
    await fs.navigateTo('/network/n');
    await fs.upload([new File(['abc'], 'a.pdf'), new File(['abc'], 'b.pdf')]);
    expect(mocked.post).toHaveBeenNthCalledWith(
      1,
      '/draft/files',
      expect.objectContaining({
        id: 'tmp1',
        fileName: 'a.pdf',
        path: '/network/n',
        justUploaded: true,
        timestamps: undefined,
      }),
    );
    expect(mocked.post).toHaveBeenNthCalledWith(2, '/draft/files', {
      fileName: 'b.pdf',
      path: '/network/n',
      id: 'tmp1',
      size: 3,
      md5: 'x',
      justUploaded: true,
    });
  });

  it('renames the current document and follows it', async () => {
    const fs = useFileSystem('/');
    await fs.navigateTo('/network/n/docs');
    await fs.rename(fs.document.value as FileDto, 'papers');
    expect(mocked.put).toHaveBeenCalledWith('/draft/file/network/n/docs', null, { params: { name: 'papers' } });
    expect(fs.path.value).toBe('/network/n/papers');
  });

  it('applies the status operations to the selection, or to the current document', async () => {
    const a = file('/network/n/a.pdf', { revisionStatus: 'UNDER_REVIEW' });
    const b = file('/network/n/b.pdf');
    mocked.get.mockImplementation(async () => ({ data: folder('/network/n', [a, b]) }));
    const fs = useFileSystem('/network/n');
    await fs.navigateTo('/network/n');
    fs.selected.value = [a, b];
    await fs.publish(true);
    // only the file under review can be published
    expect(mocked.put).toHaveBeenCalledTimes(1);
    expect(mocked.put).toHaveBeenCalledWith('/draft/file/network/n/a.pdf', null, { params: { publish: true } });
    fs.selected.value = [];
    await fs.toStatus('UNDER_REVIEW');
    expect(mocked.put).toHaveBeenLastCalledWith('/draft/file/network/n', null, { params: { status: 'UNDER_REVIEW' } });
  });

  it('deletes the selected deleted files, ignoring conflicts', async () => {
    const a = file('/network/n/a.pdf', { revisionStatus: 'DELETED' });
    const b = file('/network/n/b.pdf', { revisionStatus: 'DELETED' });
    mocked.get.mockImplementation(async () => ({ data: folder('/network/n', [a, b]) }));
    mocked.delete.mockImplementation(async (url: string) => {
      if (url.endsWith('b.pdf')) throw { response: { status: 409 } };
      return {};
    });
    const fs = useFileSystem('/network/n');
    await fs.navigateTo('/network/n');
    fs.selected.value = [a, b];
    await fs.remove();
    expect(mocked.delete).toHaveBeenCalledTimes(2);
    expect(notifyError).not.toHaveBeenCalled();
    expect(fs.path.value).toBe('/network/n');
  });

  it('searches the current folder, by text or shortcut, and reruns the search on refresh', async () => {
    const found = file('/network/n/docs/a.pdf');
    mocked.get.mockImplementation(async (url: string) =>
      url.includes('files-search') ? { data: [found] } : { data: folder('/network/n') },
    );
    const fs = useFileSystem('/network/n');
    await fs.navigateTo('/network/n');
    await fs.searchFiles(' a ', true);
    expect(mocked.get).toHaveBeenLastCalledWith('/draft/files-search/network/n', {
      params: { query: 'a', recursively: true, limit: 999 },
    });
    expect(fs.searching.value).toBe(true);
    expect(fs.children.value).toEqual([found]);
    await fs.searchShortcut('RECENT', false);
    expect(mocked.get).toHaveBeenLastCalledWith('/draft/files-search/network/n', {
      params: { query: '', recursively: false, limit: 10, sort: 'lastModifiedDate', order: 'desc' },
    });
    await fs.searchShortcut('NOT_PUBLISHED');
    expect(mocked.get).toHaveBeenLastCalledWith('/draft/files-search/network/n', {
      params: { query: 'NOT(publicationDate:*)', recursively: false, limit: 999 },
    });
    await fs.refresh();
    expect(mocked.get).toHaveBeenLastCalledWith('/draft/files-search/network/n', expect.anything());
    await fs.clearSearch();
    expect(fs.searching.value).toBe(false);
    expect(mocked.get).toHaveBeenLastCalledWith('/draft/file/network/n');
  });

  it('cuts files and pastes them into another folder, folders as subfolders', async () => {
    const a = file('/network/n/a.pdf');
    const sub = folder('/network/n/sub');
    const locked = file('/network/n/b.pdf', { revisionStatus: 'UNDER_REVIEW' });
    mocked.get.mockImplementation(async (url: string) => ({
      data: url.endsWith('/docs') ? folder('/network/n/docs') : folder('/network/n', [a, sub, locked]),
    }));
    const fs = useFileSystem('/');
    const clipboard = useFilesStore();
    await fs.navigateTo('/network/n');
    fs.selected.value = [a, sub, locked];
    fs.cutToClipboard();
    // the file under review cannot be moved
    expect(clipboard.items.map((i) => i.name)).toEqual(['a.pdf', 'sub']);
    expect(fs.canPaste.value).toBe(false);
    await fs.paste();
    expect(mocked.put).not.toHaveBeenCalled();
    await fs.navigateTo('/network/n/docs');
    expect(fs.canPaste.value).toBe(true);
    await fs.paste();
    expect(mocked.put).toHaveBeenCalledWith('/draft/file/network/n/a.pdf', null, {
      params: { move: '/network/n/docs' },
    });
    expect(mocked.put).toHaveBeenCalledWith('/draft/file/network/n/sub', null, {
      params: { move: '/network/n/docs/sub' },
    });
    expect(clipboard.hasItems).toBe(false);
    fs.copyToClipboard();
    expect(clipboard.command).toBe('copy');
    expect(clipboard.items.map((i) => i.path)).toEqual(['/network/n/docs']);
  });

  it('restores a revision and updates the details of the current file', async () => {
    const current = file('/network/n/a.pdf', { size: 10 });
    mocked.get.mockImplementation(async () => ({ data: current }));
    const fs = useFileSystem('/network/n');
    await fs.navigateTo('/network/n/a.pdf');
    await fs.restoreRevision({ id: 'v1', fileName: 'a.pdf', description: [], attributes: [] });
    expect(mocked.put).toHaveBeenCalledWith('/draft/file/network/n/a.pdf', null, { params: { version: 'v1' } });
    await fs.updateDetails({ type: 'report', description: [{ lang: 'en', value: 'Yearly report' }] });
    expect(mocked.post).toHaveBeenCalledWith('/draft/files', {
      id: 'v2',
      fileName: 'a.pdf',
      path: '/network/n',
      attributes: [],
      size: 10,
      type: 'report',
      description: [{ lang: 'en', value: 'Yearly report' }],
      timestamps: undefined,
    });
  });
});
