<template>
  <div>
    <div class="text-h6 q-mb-xs">{{ t('config.roles') }}</div>
    <div class="text-hint q-mb-sm">{{ t('config.roles_help') }}</div>
    <q-btn
      v-if="authStore.isAdministrator"
      color="primary"
      icon="add"
      :label="t('config.add_role')"
      size="sm"
      class="q-mb-sm"
      @click="showAdd = true"
    />
    <q-table
      flat
      dense
      :rows="rows"
      :columns="columns"
      row-key="id"
      :pagination="{ rowsPerPage: 20 }"
      :rows-per-page-options="[20, 50]"
      :no-data-label="t('config.roles_none')"
      :hide-pagination="rows.length <= 20"
    >
      <template v-slot:body-cell-actions="props">
        <q-td :props="props" class="text-no-wrap">
          <q-btn
            flat
            dense
            round
            size="sm"
            icon="delete"
            color="negative"
            :title="t('delete')"
            @click="onDeleteRequest(props.row.id)"
          />
        </q-td>
      </template>
    </q-table>
    <role-dialog v-model="showAdd" :roles="roles" :saving="saving" @save="onAdd" />
    <confirm-dialog
      v-model="showDelete"
      :title="t('config.delete_role_title')"
      :text="t('config.delete_role_text', { role: toDelete })"
      @confirm="onDelete"
    />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import RoleDialog from 'src/components/settings/general/RoleDialog.vue';
import { copyConfig } from 'src/utils/config';
import { notifyError, notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();

const saving = ref(false);
const showAdd = ref(false);
const showDelete = ref(false);
const toDelete = ref('');

const roles = computed(() => systemStore.configuration.roles || []);
const rows = computed(() => roles.value.map((id) => ({ id })));

const columns = computed<QTableColumn[]>(() => [
  { name: 'id', label: t('config.role_id'), field: 'id', align: 'left', sortable: true },
  ...(authStore.isAdministrator
    ? [{ name: 'actions', label: t('history.actions'), field: 'id', align: 'left' } as QTableColumn]
    : []),
]);

/** the roles are part of the configuration: it is saved whole */
async function saveRoles(list: string[]): Promise<boolean> {
  saving.value = true;
  try {
    const config = copyConfig(systemStore.configuration);
    config.roles = list;
    await systemStore.save(config);
    notifySuccess('config.saved');
    return true;
  } catch (error) {
    notifyError(error);
    return false;
  } finally {
    saving.value = false;
  }
}

async function onAdd(role: string) {
  if (await saveRoles([...roles.value, role])) showAdd.value = false;
}

function onDeleteRequest(role: string) {
  toDelete.value = role;
  showDelete.value = true;
}

async function onDelete() {
  await saveRoles(roles.value.filter((role) => role !== toDelete.value));
}
</script>
