<template>
  <div class="row q-col-gutter-lg">
    <div :class="openAccess ? 'col-12' : 'col-12 col-md-6'">
      <acl-table
        :acls="permissions"
        :title="t('permission.draft')"
        :help="t('permission.help')"
        :add-label="t('permission.add')"
        :empty-label="t('permission.none')"
        with-role
        :loading="loadingPermissions"
        :can-edit="canEdit"
        @add="onAddPermission"
        @edit="onEditPermission"
        @delete="onDeleteRequest('permissions', $event)"
      />
    </div>
    <div v-if="!openAccess" class="col-12 col-md-6">
      <acl-table
        :acls="accesses"
        :title="t('access.published')"
        :help="t('access.help')"
        :add-label="t('access.add')"
        :empty-label="t('access.none')"
        :loading="loadingAccesses"
        :can-edit="canEdit"
        @add="onAddAccess"
        @delete="onDeleteRequest('accesses', $event)"
      />
    </div>
    <acl-dialog
      v-model="showPermission"
      :acl="editedPermission"
      with-role
      :title="permissionTitle"
      :principal-hint="t('permission.principal_help')"
      :file-label="t('permission.file_permission')"
      :saving="saving"
      @save="onSavePermission"
    />
    <acl-dialog
      v-model="showAccess"
      :title="t('access.add')"
      :principal-hint="t('access.principal_help')"
      :file-label="t('permission.file_access')"
      :saving="saving"
      @save="onSaveAccess"
    />
    <confirm-dialog
      v-model="showDelete"
      :title="toDelete?.kind === 'permissions' ? t('permission.delete_title') : t('access.delete_title')"
      :text="deleteText"
      @confirm="onDelete"
    />
  </div>
</template>

<script setup lang="ts">
import type { AclDto } from 'src/models/MicaSecurity';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import AclTable from 'src/components/permissions/AclTable.vue';
import AclDialog from 'src/components/permissions/AclDialog.vue';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentAcl, type AclInput } from 'src/composables/useDocumentAcl';

interface Props {
  target: DocumentTarget;
  /** the user can change the lists (publish permission) */
  canEdit?: boolean;
}

const props = defineProps<Props>();
const { t } = useI18n();
const systemStore = useSystemStore();

const {
  permissions,
  accesses,
  loadingPermissions,
  loadingAccesses,
  loadPermissions,
  loadAccesses,
  savePermission,
  deletePermission,
  saveAccess,
  deleteAccess,
} = useDocumentAcl(() => props.target);

/** the publications are readable by anyone: no accesses to manage */
const openAccess = computed(() => systemStore.configuration.openAccess === true);

const saving = ref(false);
const showPermission = ref(false);
const editedPermission = ref<AclDto>();
const showAccess = ref(false);
const showDelete = ref(false);
const toDelete = ref<{ kind: 'permissions' | 'accesses'; acl: AclDto }>();

const permissionTitle = computed(() =>
  !editedPermission.value
    ? t('permission.add')
    : editedPermission.value.type === 'USER'
      ? t('permission.edit_user')
      : t('permission.edit_group'),
);

const deleteText = computed(() => {
  const acl = toDelete.value?.acl;
  if (!acl) return '';
  const key = toDelete.value?.kind === 'permissions' ? 'permission.delete_text' : 'access.delete_text';
  return t(key, { type: t(`permission.${acl.type.toLowerCase()}`).toLowerCase(), principal: acl.principal });
});

function onAddPermission() {
  editedPermission.value = undefined;
  showPermission.value = true;
}

function onEditPermission(acl: AclDto) {
  editedPermission.value = acl;
  showPermission.value = true;
}

async function onSavePermission(acl: AclInput) {
  saving.value = true;
  try {
    if (await savePermission(acl)) showPermission.value = false;
  } finally {
    saving.value = false;
  }
}

function onAddAccess() {
  showAccess.value = true;
}

async function onSaveAccess(acl: AclInput) {
  saving.value = true;
  try {
    if (await saveAccess(acl)) showAccess.value = false;
  } finally {
    saving.value = false;
  }
}

function onDeleteRequest(kind: 'permissions' | 'accesses', acl: AclDto) {
  toDelete.value = { kind, acl };
  showDelete.value = true;
}

async function onDelete() {
  if (!toDelete.value) return;
  const { kind, acl } = toDelete.value;
  await (kind === 'permissions' ? deletePermission(acl) : deleteAccess(acl));
}

watch(
  () => props.target.id,
  () => {
    loadPermissions();
    if (!openAccess.value) loadAccesses();
  },
  { immediate: true },
);
</script>
