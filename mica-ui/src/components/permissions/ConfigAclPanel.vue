<template>
  <div>
    <acl-table
      :acls="permissions"
      :title="title"
      :help="help"
      :add-label="t('permission.add')"
      :empty-label="t('permission.none')"
      with-role
      :other-resource-labels="otherResourceLabels"
      :loading="loadingPermissions"
      :can-edit="canEdit"
      @add="onAdd"
      @edit="onEdit"
      @delete="onDeleteRequest"
    />
    <acl-dialog
      v-model="showDialog"
      :acl="edited"
      with-role
      :roles="roles"
      :title="dialogTitle"
      :principal-hint="t('permission.principal_help')"
      :file-label="fileLabel"
      :other-resources="otherResources"
      :saving="saving"
      @save="onSave"
    />
    <confirm-dialog v-model="showDelete" :title="t('permission.delete_title')" :text="deleteText" @confirm="onDelete" />
  </div>
</template>

<script setup lang="ts">
import type { AclDto } from 'src/models/MicaSecurity';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import AclTable from 'src/components/permissions/AclTable.vue';
import AclDialog, { type OtherResourceOption, type RoleOption } from 'src/components/permissions/AclDialog.vue';
import { useConfigAcl, type AclInput } from 'src/composables/useAcl';

interface Props {
  /** the permissions resource of the configuration (`/config/contingencies/permissions`...) */
  endpoint: string;
  title: string;
  /** may contain HTML, from the app bundles */
  help: string;
  /** the roles that can be granted on the resource */
  roles: RoleOption[];
  /** the label of the "apply to files" option; none when the resource has no files */
  fileLabel?: string | undefined;
  /** the other resources a permission can be granted on too */
  otherResources?: OtherResourceOption[] | undefined;
  canEdit?: boolean | undefined;
}

const props = defineProps<Props>();
const { t } = useI18n();

const { permissions, loadingPermissions, loadPermissions, savePermission, deletePermission } = useConfigAcl(
  () => props.endpoint,
  () => ({
    withFile: props.fileLabel !== undefined,
    otherResources: props.otherResources?.map((option) => option.value),
  }),
);

const otherResourceLabels = computed(() =>
  props.otherResources
    ? Object.fromEntries(props.otherResources.map((option) => [option.value, option.label]))
    : undefined,
);

const saving = ref(false);
const showDialog = ref(false);
const edited = ref<AclDto>();
const showDelete = ref(false);
const toDelete = ref<AclDto>();

const dialogTitle = computed(() =>
  !edited.value
    ? t('permission.add')
    : edited.value.type === 'USER'
      ? t('permission.edit_user')
      : t('permission.edit_group'),
);

const deleteText = computed(() => {
  const acl = toDelete.value;
  return acl
    ? t('permission.delete_text', {
        type: t(`permission.${acl.type.toLowerCase()}`).toLowerCase(),
        principal: acl.principal,
      })
    : '';
});

function onAdd() {
  edited.value = undefined;
  showDialog.value = true;
}

function onEdit(acl: AclDto) {
  edited.value = acl;
  showDialog.value = true;
}

async function onSave(acl: AclInput) {
  saving.value = true;
  try {
    if (await savePermission(acl)) showDialog.value = false;
  } finally {
    saving.value = false;
  }
}

function onDeleteRequest(acl: AclDto) {
  toDelete.value = acl;
  showDelete.value = true;
}

async function onDelete() {
  if (toDelete.value) await deletePermission(toDelete.value);
}

watch(() => props.endpoint, loadPermissions, { immediate: true });
</script>
