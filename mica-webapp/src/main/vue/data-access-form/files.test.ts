import { describe, it, expect, vi, afterEach } from 'vitest';
import { createFileUploadHooks } from './files';

const response = (init: { ok?: boolean; status?: number; headers?: Record<string, string>; body?: unknown }) => ({
  ok: init.ok ?? true,
  status: init.status ?? 200,
  headers: { get: (name: string) => (init.headers || {})[name] ?? null },
  json: async () => init.body,
});

describe('file upload hooks', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('uploads to the temp store and reads the file metadata from the Location', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(response({ status: 201, headers: { Location: 'http://mica/ws/files/temp/tmp-1' } }))
      .mockResolvedValueOnce(response({ body: { id: 'tmp-1', fileName: 'doc.pdf', size: 12, md5: 'x' } }));
    vi.stubGlobal('fetch', fetch);
    const file = new File(['%PDF'], 'doc.pdf', { type: 'application/pdf' });
    const item = await createFileUploadHooks('/mica').upload!(file, {} as any);
    expect(item).toEqual({ id: 'tmp-1', fileName: 'doc.pdf', size: 12, md5: 'x', justUploaded: true });
    expect(fetch.mock.calls[0][0]).toBe('/mica/ws/files/temp');
    expect(fetch.mock.calls[0][1].method).toBe('POST');
    expect(fetch.mock.calls[0][1].body).toBeInstanceOf(FormData);
    expect(fetch.mock.calls[1][0]).toBe('/mica/ws/files/temp/tmp-1');
  });

  it('fails on an upload error or a missing Location', async () => {
    const hooks = createFileUploadHooks('');
    const file = new File(['x'], 'x.txt');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response({ ok: false, status: 413 })));
    await expect(hooks.upload!(file, {} as any)).rejects.toThrow('413');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response({ status: 201 })));
    await expect(hooks.upload!(file, {} as any)).rejects.toThrow('no file location');
  });

  it('deletes just-uploaded temp files only, and reports a rejected deletion', async () => {
    const fetch = vi.fn().mockResolvedValue(response({ status: 204 }));
    vi.stubGlobal('fetch', fetch);
    const hooks = createFileUploadHooks('');
    await hooks.remove!({ id: 'stored', fileName: 'a.pdf', path: '/data-access-request/1' }, {} as any);
    expect(fetch).not.toHaveBeenCalled();
    await hooks.remove!({ id: 'tmp-1', fileName: 'a.pdf', justUploaded: true }, {} as any);
    expect(fetch).toHaveBeenCalledWith('/ws/files/temp/tmp-1', { method: 'DELETE' });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({ ok: false, status: 404 })));
    await expect(hooks.remove!({ id: 'tmp-2', justUploaded: true }, {} as any)).rejects.toThrow('404');
  });

  it('builds the download URL of stored files only', () => {
    const hooks = createFileUploadHooks('/mica');
    expect(hooks.downloadUrl!({ id: 'f1', fileName: 'my file.pdf', path: '/data-access-request/1/amendment/1-A1' }, {} as any))
      .toBe('/mica/ws/data-access-request/1/amendment/1-A1/form/attachments/my%20file.pdf/f1/_download');
    expect(hooks.downloadUrl!({ id: 'tmp', fileName: 'x.pdf', justUploaded: true }, {} as any)).toBeUndefined();
  });
});
