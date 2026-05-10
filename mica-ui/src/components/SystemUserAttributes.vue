<template>
  <div>
    <q-table
      :rows="attributes"
      flat
      row-key="name"
      :columns="columns"
      :pagination="initialPagination"
      :hide-pagination="attributes.length <= initialPagination.rowsPerPage"
    >
      <template v-slot:top-left>
        <q-btn size="sm" icon="add" color="primary" :label="t('add')" @click="onAdd" />
      </template>
      <template v-slot:top-right>
        <q-input v-model="filter" debounce="300" :placeholder="t('search')" dense clearable class="q-mr-md">
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>
      <template v-slot:body="props">
        <q-tr :props="props" @mouseover="onOverRow(props.row)" @mouseleave="onLeaveRow(props.row)">
          <q-td key="name" :props="props">
            <span class="text-primary">{{ props.row.name }}</span>
            <div class="float-right">
              <q-btn
                rounded
                dense
                flat
                size="sm"
                color="secondary"
                :icon="toolsVisible[props.row.name] ? 'edit' : 'none'"
                :title="t('edit')"
                class="q-ml-xs"
                @click="onShowEdit(props.row)"
              />
              <q-btn
                rounded
                dense
                flat
                size="sm"
                color="secondary"
                :title="t('delete')"
                :icon="toolsVisible[props.row.name] ? 'delete' : 'none'"
                class="q-ml-xs"
                @click="onShowDelete(props.row)"
              />
            </div>
          </q-td>
          <q-td key="type" :props="props">
            <span>{{ t(`system.attributes.types.${props.row.type}`) }}</span>
          </q-td>
          <q-td key="description" :props="props">
            <span>{{ props.row.description }}</span>
          </q-td>
          <q-td key="values" :props="props" @mouseover="onOverRow(props.row)" @mouseleave="onLeaveRow(props.row)">
            <q-chip size="sm" class="q-ml-none" v-for="(value, index) in props.row.values" :key="index">
              {{ value }}
            </q-chip>
          </q-td>
          <q-td key="required" :props="props">
            <q-icon :name="props.row.required ? 'check_box' : 'check_box_outline_blank'" size="sm" dense />
          </q-td>
        </q-tr>
      </template>
    </q-table>

    <confirm-dialog
      v-model="showDelete"
      :title="t('system.attributes.remove')"
      :text="t('system.attributes.remove_confirm', { name: selected?.name })"
      @confirm="onDelete"
    />

    <system-user-attributes-dialog
      v-model="showEdit"
      :attribute="selected"
      @saved="onSavedAttribute"
      @cancel="onCancel"
    />
  </div>
</template>

<script setup lang="ts">
import type { AttributeConfigurationDto } from 'src/models/Mica';
import { DefaultAlignment } from 'src/components/models';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import SystemUserAttributesDialog from 'src/components/SystemUserAttributesDialog.vue';

const systemStore = useSystemStore();
const { t } = useI18n();

// const showDialog = ref(false);
const toolsVisible = ref<{ [key: string]: boolean }>({});
const initialPagination = ref({
  descending: false,
  page: 1,
  rowsPerPage: 20,
});
const attributes = computed(
  () =>
    systemStore.userAttributes?.filter((attr) =>
      filter.value ? attr.name.toLowerCase().includes(filter.value.toLowerCase()) : true,
    ) || [],
);
const filter = ref('');
const selected = ref();
const showEdit = ref(false);
const showDelete = ref(false);

const columns = computed(() => [
  { name: 'name', label: t('name'), field: 'name', align: DefaultAlignment, sortable: true },
  { name: 'type', label: t('type'), field: 'type', align: DefaultAlignment, sortable: true },
  { name: 'description', label: t('description'), field: 'description', align: DefaultAlignment, sortable: true },
  {
    name: 'values',
    label: t('values'),
    field: 'values',
    format: (val: string) => (val || '').split(/\s*,\s*/),
    align: DefaultAlignment,
  },
  { name: 'required', label: t('required'), field: 'required', align: DefaultAlignment, sortable: true },
]);

function onOverRow(row: AttributeConfigurationDto) {
  toolsVisible.value[row.name] = true;
}

function onLeaveRow(row: AttributeConfigurationDto) {
  toolsVisible.value[row.name] = false;
}

function onAdd() {
  selected.value = undefined;
  showEdit.value = true;
}

function onShowEdit(row: AttributeConfigurationDto) {
  selected.value = row;
  showEdit.value = true;
}

function onShowDelete(row: AttributeConfigurationDto) {
  selected.value = row;
  showDelete.value = true;
}

function onDelete() {
  if (selected.value) {
    systemStore.removeAttribute(selected.value);
  }
}

function onCancel() {
  showEdit.value = false;
}

function onSavedAttribute() {
  selected.value = undefined;
  showEdit.value = false;
  systemStore.init();
}
</script>
