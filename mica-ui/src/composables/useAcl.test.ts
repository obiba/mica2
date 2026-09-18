import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('src/boot/api', () => ({ api: { get: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import { documentTarget } from './useDocumentTarget';
import { useDocumentAcl, useFileAcl } from './useAcl';

const target = documentTarget('network', 'net1');
const mocked = api as unknown as {
  get: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

beforeEach(() => {
  vi.clearAllMocks();
  mocked.get.mockResolvedValue({ data: [{ principal: 'p', type: 'USER', role: 'READER' }] });
  mocked.put.mockResolvedValue({});
  mocked.delete.mockResolvedValue({});
});

describe('useDocumentAcl', () => {
  it('loads the permissions and the accesses', async () => {
    const acl = useDocumentAcl(target);
    expect(await acl.loadPermissions()).toHaveLength(1);
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/permissions');
    await acl.loadAccesses();
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/accesses');
    expect(acl.accesses.value).toHaveLength(1);
  });

  it('saves a permission with its role and reloads', async () => {
    const acl = useDocumentAcl(target);
    expect(await acl.savePermission({ principal: 'bob', type: 'USER', role: 'EDITOR', file: true })).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/permissions', null, {
      params: { principal: 'bob', type: 'USER', file: true, role: 'EDITOR' },
    });
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/permissions');
  });

  it('saves an access without a role', async () => {
    const acl = useDocumentAcl(target);
    await acl.saveAccess({ principal: '*', type: 'GROUP', file: false });
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/accesses', null, {
      params: { principal: '*', type: 'GROUP', file: false },
    });
  });

  it('manages the ACLs of a file, without the file option', async () => {
    const acl = useFileAcl('/network/net1/docs/a b.pdf');
    await acl.loadPermissions();
    expect(mocked.get).toHaveBeenCalledWith('/draft/file-permission/network/net1/docs/a%20b.pdf');
    await acl.savePermission({ principal: 'bob', type: 'USER', role: 'EDITOR' });
    expect(mocked.put).toHaveBeenCalledWith('/draft/file-permission/network/net1/docs/a%20b.pdf', null, {
      params: { principal: 'bob', type: 'USER', role: 'EDITOR' },
    });
    await acl.deleteAccess({ principal: 'bob', type: 'USER' });
    expect(mocked.delete).toHaveBeenCalledWith('/draft/file-access/network/net1/docs/a%20b.pdf', {
      params: { principal: 'bob', type: 'USER' },
    });
  });

  it('deletes and reports failures', async () => {
    const acl = useDocumentAcl(target);
    expect(await acl.deleteAccess({ principal: 'bob', type: 'USER' })).toBe(true);
    expect(mocked.delete).toHaveBeenCalledWith('/draft/network/net1/accesses', {
      params: { principal: 'bob', type: 'USER' },
    });
    mocked.delete.mockRejectedValueOnce(new Error('nope'));
    expect(await acl.deletePermission({ principal: 'bob', type: 'USER' })).toBe(false);
    expect(notifyError).toHaveBeenCalled();
  });
});
