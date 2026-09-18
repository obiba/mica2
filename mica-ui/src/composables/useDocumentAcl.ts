import type { MaybeRefOrGetter } from 'vue';
import { api } from 'src/boot/api';
import type { AclDto } from 'src/models/MicaSecurity';
import { notifyError } from 'src/utils/notify';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';

export type AclType = 'USER' | 'GROUP';
/** the roles on a draft document */
export const DOCUMENT_ROLES = ['READER', 'EDITOR', 'REVIEWER'] as const;
export type DocumentRole = (typeof DOCUMENT_ROLES)[number];

/** what the dialogs edit: a permission (with a role) or an access (read-only on the publication) */
export interface AclInput {
  principal: string;
  type: AclType;
  role?: DocumentRole | undefined;
  /** apply to the files of the document too */
  file: boolean;
}

/**
 * The access control lists of a draft document: the permissions on the draft (with a role) and
 * the accesses to the publication.
 */
export function useDocumentAcl(target: MaybeRefOrGetter<DocumentTarget>) {
  const permissions = ref<AclDto[]>([]);
  const accesses = ref<AclDto[]>([]);
  const loadingPermissions = ref(false);
  const loadingAccesses = ref(false);

  function path(kind: 'permissions' | 'accesses') {
    return `${toValue(target).path}/${kind}`;
  }

  async function load(kind: 'permissions' | 'accesses'): Promise<AclDto[]> {
    const list = kind === 'permissions' ? permissions : accesses;
    const loading = kind === 'permissions' ? loadingPermissions : loadingAccesses;
    loading.value = true;
    try {
      const response = await api.get<AclDto[]>(path(kind));
      list.value = response.data;
    } catch (error) {
      notifyError(error);
      list.value = [];
    } finally {
      loading.value = false;
    }
    return list.value;
  }

  async function save(kind: 'permissions' | 'accesses', acl: AclInput): Promise<boolean> {
    try {
      const params: Record<string, string | boolean> = { principal: acl.principal, type: acl.type, file: acl.file };
      if (kind === 'permissions' && acl.role) params.role = acl.role;
      await api.put(path(kind), null, { params });
      await load(kind);
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  async function remove(kind: 'permissions' | 'accesses', acl: Pick<AclDto, 'principal' | 'type'>): Promise<boolean> {
    try {
      await api.delete(path(kind), { params: { principal: acl.principal, type: acl.type } });
      await load(kind);
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  return {
    permissions,
    accesses,
    loadingPermissions,
    loadingAccesses,
    loadPermissions: () => load('permissions'),
    loadAccesses: () => load('accesses'),
    savePermission: (acl: AclInput) => save('permissions', acl),
    deletePermission: (acl: Pick<AclDto, 'principal' | 'type'>) => remove('permissions', acl),
    saveAccess: (acl: AclInput) => save('accesses', acl),
    deleteAccess: (acl: Pick<AclDto, 'principal' | 'type'>) => remove('accesses', acl),
  };
}
