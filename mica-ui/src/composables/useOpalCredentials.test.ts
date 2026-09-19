import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { OpalCredentialType, type OpalCredentialDto } from 'src/models/Mica';
import { opalCredentialCertificateUrl, useOpalCredentials } from './useOpalCredentials';
import { saveSystemKey, systemCertificateUrl } from './useKeyStore';
import { KeyType } from 'src/models/Mica';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  toServerUrl: (path: string) => `/ws${path}`,
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;
const credential: OpalCredentialDto = {
  type: OpalCredentialType.USERNAME,
  opalUrl: 'https://opal.example.org',
  username: 'mica',
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useOpalCredentials', () => {
  it('lists the credentials', async () => {
    mocked.get.mockResolvedValueOnce({ data: [credential] });
    const { credentials, load } = useOpalCredentials();
    await load();
    expect(mocked.get).toHaveBeenCalledWith('/config/opal-credentials');
    expect(credentials.value).toEqual([credential]);
  });

  it('saves a credential then reloads the list', async () => {
    mocked.post.mockResolvedValueOnce({ status: 201 });
    mocked.get.mockResolvedValueOnce({ data: [credential] });
    const { credentials, save } = useOpalCredentials();
    expect(await save(credential)).toBe(true);
    expect(mocked.post).toHaveBeenCalledWith('/config/opal-credentials', credential);
    expect(credentials.value).toEqual([credential]);
  });

  it('reports a failed save without reloading', async () => {
    mocked.post.mockRejectedValueOnce(new Error('400'));
    const { save } = useOpalCredentials();
    expect(await save(credential)).toBe(false);
    expect(mocked.get).not.toHaveBeenCalled();
  });

  it('deletes a credential by its Opal URL', async () => {
    mocked.delete.mockResolvedValueOnce({ status: 200 });
    mocked.get.mockResolvedValueOnce({ data: [] });
    const { credentials, remove } = useOpalCredentials();
    expect(await remove(credential)).toBe(true);
    expect(mocked.delete).toHaveBeenCalledWith('/config/opal-credential', {
      params: { id: 'https://opal.example.org' },
    });
    expect(credentials.value).toEqual([]);
  });

  it('builds the certificate URLs', () => {
    expect(opalCredentialCertificateUrl('https://opal.example.org/')).toBe(
      '/ws/config/opal-credential/certificate?id=https%3A%2F%2Fopal.example.org%2F',
    );
    expect(systemCertificateUrl()).toBe('/ws/config/keystore/system/https');
  });

  it('saves the system key', async () => {
    mocked.put.mockResolvedValueOnce({ status: 200 });
    const keyForm = { keyType: KeyType.KEY_PAIR, privateImport: 'PRIVATE', publicImport: 'PUBLIC' };
    await saveSystemKey(keyForm);
    expect(mocked.put).toHaveBeenCalledWith('/config/keystore/system/https', keyForm);
  });
});
