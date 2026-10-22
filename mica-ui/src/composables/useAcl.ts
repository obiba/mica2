import type { MaybeRefOrGetter } from 'vue';
import { api } from 'src/boot/api';
import type { AclDto } from 'src/models/MicaSecurity';
import { notifyError } from 'src/utils/notify';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { encodePath } from 'src/utils/files';

export type AclType = 'USER' | 'GROUP';
/** the roles on a draft document */
export const DOCUMENT_ROLES = ['READER', 'EDITOR', 'REVIEWER'] as const;
export type DocumentRole = (typeof DOCUMENT_ROLES)[number];

/** what the dialogs edit: a permission (with a role) or an access (read-only on the publication) */
export interface AclInput {
  principal: string;
  type: AclType;
  /** a document role, or the role of a configuration resource (`ANALYST`...) */
  role?: string | undefined;
  /** apply to the files of the document too (documents only) */
  file?: boolean | undefined;
  /** the other resources of the permission granted too (`action-logs`... of the data access requests) */
  otherResources?: string[] | undefined;
}

/** where the lists are managed: the permissions (draft) and the accesses (publication) resources */
export interface AclEndpoints {
  permissions: string;
  /** none for a configuration resource, which has no publication */
  accesses?: string | undefined;
  /** fixed query parameters of the permission save */
  params?: Record<string, string | boolean> | undefined;
  /** the other resources a permission can be granted on too, sent as repeated `otherResources` parameters */
  otherResources?: string[] | undefined;
}

/** the options of a configuration resource */
export interface ConfigAclOptions {
  /** the permission can apply to the files of the resource (the `file` parameter, not sent as false) */
  withFile?: boolean;
  /** the other resources a permission can be granted on too */
  otherResources?: string[] | undefined;
}

export function documentAclEndpoints(target: DocumentTarget): AclEndpoints {
  return { permissions: `${target.path}/permissions`, accesses: `${target.path}/accesses` };
}

/**
 * A configuration resource (Opal views download, contingency tables...): permissions only, saved
 * with the role as is (`config`) and without file permission.
 */
export function configAclEndpoints(path: string, options: ConfigAclOptions = {}): AclEndpoints {
  const endpoints: AclEndpoints = {
    permissions: path,
    params: options.withFile ? { config: true } : { config: true, file: false },
  };
  if (options.otherResources) endpoints.otherResources = options.otherResources;
  return endpoints;
}

export function fileAclEndpoints(path: string): AclEndpoints {
  return {
    permissions: `/draft/file-permission${encodePath(path)}`,
    accesses: `/draft/file-access${encodePath(path)}`,
  };
}

/**
 * The access control lists of a draft document or file: the permissions on the draft (with a
 * role) and the accesses to the publication.
 */
export function useAcl(endpoints: MaybeRefOrGetter<AclEndpoints>) {
  const permissions = ref<AclDto[]>([]);
  const accesses = ref<AclDto[]>([]);
  const loadingPermissions = ref(false);
  const loadingAccesses = ref(false);

  function path(kind: 'permissions' | 'accesses'): string | undefined {
    return toValue(endpoints)[kind];
  }

  async function load(kind: 'permissions' | 'accesses'): Promise<AclDto[]> {
    const list = kind === 'permissions' ? permissions : accesses;
    const loading = kind === 'permissions' ? loadingPermissions : loadingAccesses;
    const resource = path(kind);
    if (!resource) {
      list.value = [];
      return list.value;
    }
    loading.value = true;
    try {
      const response = await api.get<AclDto[]>(resource);
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
    const resource = path(kind);
    if (!resource) return false;
    try {
      const params: Record<string, string | boolean | string[]> = {
        ...(kind === 'permissions' ? toValue(endpoints).params : {}),
        principal: acl.principal,
        type: acl.type,
      };
      if (acl.file !== undefined) params.file = acl.file;
      if (kind === 'permissions' && acl.role) params.role = acl.role;
      if (kind === 'permissions' && acl.otherResources) params.otherResources = acl.otherResources;
      // a list as repeated parameters (`otherResources=a&otherResources=b`), as the server reads them
      await api.put(resource, null, { params, paramsSerializer: { indexes: null } });
      await load(kind);
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  async function remove(kind: 'permissions' | 'accesses', acl: Pick<AclDto, 'principal' | 'type'>): Promise<boolean> {
    const resource = path(kind);
    if (!resource) return false;
    try {
      await api.delete(resource, { params: { principal: acl.principal, type: acl.type } });
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

/** the ACLs of a draft document (`/draft/{type}/{id}/permissions|accesses`) */
export function useDocumentAcl(target: MaybeRefOrGetter<DocumentTarget>) {
  return useAcl(() => documentAclEndpoints(toValue(target)));
}

/** the ACLs of a draft file or folder (`/draft/file-permission|file-access/{path}`) */
export function useFileAcl(path: MaybeRefOrGetter<string>) {
  return useAcl(() => fileAclEndpoints(toValue(path)));
}

/** the permissions of a configuration resource (`/config/document-sets/permissions`...) */
export function useConfigAcl(path: MaybeRefOrGetter<string>, options: MaybeRefOrGetter<ConfigAclOptions> = {}) {
  return useAcl(() => configAclEndpoints(toValue(path), toValue(options)));
}
