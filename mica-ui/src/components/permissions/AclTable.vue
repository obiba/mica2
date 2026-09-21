<template>
  <div>
    <div class="text-subtitle1">{{ title }}</div>
    <div class="text-caption text-grey-7 q-mb-sm" v-html="help"></div>
    <q-btn v-if="canEdit" color="primary" icon="add" :label="addLabel" size="sm" class="q-mb-sm" @click="emit('add')" />
    <q-table
      flat
      dense
      :rows="acls"
      :columns="columns"
      :row-key="rowKey"
      :loading="loading"
      :filter="filter"
      :pagination="{ rowsPerPage: 20 }"
      :rows-per-page-options="[20, 50]"
      :no-data-label="emptyLabel"
      :hide-pagination="acls.length <= 20"
    >
      <template v-if="acls.length > 20" v-slot:top-right>
        <q-input v-model="filter" dense debounce="300" :placeholder="t('search')">
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>
      <template v-slot:body-cell-principal="props">
        <q-td :props="props">
          <em v-if="props.value === '*'">{{
            props.row.type === 'USER' ? t('permission.anyone') : t('permission.any_group')
          }}</em>
          <span v-else>{{ props.value }}</span>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props" class="text-no-wrap">
          <q-btn
            v-if="withRole"
            flat
            dense
            round
            size="sm"
            icon="edit"
            color="primary"
            :title="t('edit')"
            @click="emit('edit', props.row)"
          />
          <q-btn
            flat
            dense
            round
            size="sm"
            icon="delete"
            color="negative"
            :title="t('delete')"
            @click="emit('delete', props.row)"
          />
        </q-td>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { AclDto } from 'src/models/MicaSecurity';

interface Props {
  acls: AclDto[];
  title: string;
  /** may contain HTML, from the app bundles */
  help: string;
  addLabel: string;
  emptyLabel: string;
  /** permissions have a role and can be edited; accesses are read-only */
  withRole?: boolean;
  /** the labels of the other resources a permission can be granted on, by name: shown when any */
  otherResourceLabels?: Record<string, string> | undefined;
  loading?: boolean;
  canEdit?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ add: []; edit: [acl: AclDto]; delete: [acl: AclDto] }>();
const { t } = useI18n();

const filter = ref('');

function rowKey(acl: AclDto) {
  return `${acl.type}:${acl.principal}`;
}

const columns = computed<QTableColumn[]>(() => {
  const list: QTableColumn[] = [
    { name: 'principal', label: t('permission.principal'), field: 'principal', align: 'left', sortable: true },
    {
      name: 'type',
      label: t('type'),
      field: 'type',
      align: 'left',
      format: (value: string) => t(`permission.${value.toLowerCase()}`),
    },
  ];
  if (props.withRole) {
    list.push({
      name: 'role',
      label: t('role'),
      field: 'role',
      align: 'left',
      format: (value: string) => (value ? t(`permission.${value.toLowerCase()}`) : ''),
    });
  }
  if (props.otherResourceLabels) {
    const labels = props.otherResourceLabels;
    list.push({
      name: 'otherResources',
      label: t('permission.other_resources'),
      field: 'otherResources',
      align: 'left',
      format: (value: string[] | undefined) => (value ?? []).map((name) => labels[name] ?? name).join(', '),
    });
  }
  if (props.canEdit) {
    list.push({ name: 'actions', label: t('history.actions'), field: 'principal', align: 'left' });
  }
  return list;
});
</script>
