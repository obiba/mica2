<template>
  <div>
    <div class="text-h6">{{ t('dataset.data_schema') }}</div>
    <div class="text-hint q-mb-md">{{ t('dataset.data_schema_info') }}</div>
    <q-btn
      v-if="canEdit"
      color="primary"
      icon="edit"
      :label="t('edit')"
      size="sm"
      :disable="busy"
      class="q-mb-md"
      @click="showSchema = true"
    />
    <fields-list v-if="schema" :items="schemaItems" :dbobject="schemaRow" />

    <div class="text-h6 q-mt-lg">{{ t('dataset.harmonized_tables') }}</div>
    <div class="text-hint q-mb-md">{{ t('dataset.harmonized_tables_info') }}</div>
    <div v-if="canEdit" class="row items-center q-gutter-sm q-mb-md">
      <q-btn color="primary" icon="add" :label="t('add')" size="sm" :disable="busy" @click="onEdit(undefined)" />
      <q-btn
        color="negative"
        icon="delete"
        :label="t('network_links.remove_selected')"
        size="sm"
        :disable="busy || selected.length === 0"
        @click="toRemove = selected.map((row) => row.index)"
      />
    </div>
    <q-table
      v-model:selected="selected"
      flat
      dense
      :rows="rows"
      :columns="columns"
      row-key="index"
      :selection="canEdit ? 'multiple' : 'none'"
      :pagination="{ rowsPerPage: 0 }"
      hide-pagination
      :no-data-label="t('dataset.no_harmonized_tables')"
    >
      <template v-slot:body-cell-study="props">
        <q-td :props="props">
          <router-link :to="props.row.route" class="text-primary">{{ props.value }}</router-link>
        </q-td>
      </template>
      <template v-slot:body-cell-source="props">
        <q-td :props="props">
          <q-badge color="grey-6" :label="props.row.namespace" class="q-mr-xs" />
          {{ props.value }}
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props" class="no-wrap">
          <template v-if="canEdit">
            <q-btn
              flat
              dense
              size="sm"
              icon="arrow_upward"
              :title="t('members.move_up')"
              :disable="busy || props.row.index === 0"
              @click="onMove(props.row.index, -1)"
            />
            <q-btn
              flat
              dense
              size="sm"
              icon="arrow_downward"
              :title="t('members.move_down')"
              :disable="busy || props.row.index === rows.length - 1"
              @click="onMove(props.row.index, 1)"
            />
            <q-btn
              flat
              dense
              size="sm"
              icon="edit"
              :title="t('edit')"
              :disable="busy"
              @click="onEdit(props.row.index)"
            />
            <q-btn
              flat
              dense
              size="sm"
              icon="delete"
              color="negative"
              :title="t('delete')"
              :disable="busy"
              @click="toRemove = [props.row.index]"
            />
          </template>
        </q-td>
      </template>
    </q-table>

    <dataset-table-dialog v-model="showSchema" mode="schema" :table="schema" @save="onSchemaSave" />
    <dataset-table-dialog
      v-model="showTable"
      mode="harmonized"
      :table="edited?.table"
      :harmonization="edited?.harmonization ?? false"
      @save="onTableSave"
    />
    <confirm-dialog
      :model-value="toRemove.length > 0"
      :title="t('dataset.remove_tables')"
      :text="t('dataset.remove_tables_text', { count: toRemove.length })"
      @update:model-value="toRemove = []"
      @confirm="onRemove"
    />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { DatasetDto, DatasetDto_HarmonizationTableDto, LocalizedStringDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import FieldsList, { type FieldItem } from 'src/components/FieldsList.vue';
import DatasetTableDialog from 'src/components/datasets/DatasetTableDialog.vue';
import {
  harmonizedTables,
  sourceFields,
  sourceText,
  tableSource,
  withHarmonizedTables,
  type DatasetTable,
  type HarmonizedTable,
} from 'src/utils/datasets';
import { localized } from 'src/utils/persons';

interface Props {
  dataset: DatasetDto;
  canEdit: boolean;
  /** a change is being saved */
  busy?: boolean;
}

const props = defineProps<Props>();
/** the dataset with the tables changed, to be saved */
const emit = defineEmits<{ change: [dataset: DatasetDto] }>();
const { t, locale } = useI18n();

interface Row {
  index: number;
  study: string;
  route: string;
  type: string;
  namespace: string;
  source: string;
  name: string;
  description: string;
}

const showSchema = ref(false);
const showTable = ref(false);
/** the table edited, undefined when adding one */
const edited = ref<HarmonizedTable>();
const editedIndex = ref<number>();
const selected = ref<Row[]>([]);
const toRemove = ref<number[]>([]);

/** the data schema, required by the server */
const schema = computed(() => props.dataset.protocol?.harmonizationTable);
const tables = computed(() => harmonizedTables(props.dataset.protocol));

function label(id: string, name: LocalizedStringDto[] | undefined) {
  const text = localized(name, locale.value);
  return text ? `${id} - ${text}` : id;
}

function studyRoute(harmonization: boolean, id: string) {
  return `/${harmonization ? 'harmonization-study' : 'individual-study'}/${id}`;
}

const schemaRow = computed(() => {
  if (!schema.value) return {};
  const source = tableSource(schema.value);
  return {
    ...source,
    initiative: label(schema.value.studyId, schema.value.studySummary?.acronym),
    namespace: t(`dataset.source.${source.namespace}.title`),
  };
});
const schemaItems = computed<FieldItem[]>(() => [
  {
    field: 'initiative',
    label: 'dataset.initiative',
    links: (row) => [{ label: row.initiative, to: studyRoute(true, schema.value?.studyId ?? '') }],
  },
  { field: 'namespace', label: 'dataset.source.title' },
  ...sourceFields(tableSource(schema.value).namespace),
]);

const rows = computed<Row[]>(() =>
  tables.value.map(({ harmonization, table }, index) => {
    const source = tableSource(table);
    return {
      index,
      study: label(table.studyId, table.studySummary?.acronym),
      route: studyRoute(harmonization, table.studyId),
      type: t(harmonization ? 'dataset.initiative' : 'dataset.study'),
      namespace: t(`dataset.source.${source.namespace}.title`),
      source: sourceText(source),
      name: localized(table.name, locale.value),
      description: localized(table.description, locale.value),
    };
  }),
);
const columns = computed<QTableColumn[]>(() => [
  { name: 'study', label: t('dataset.study'), field: 'study', align: 'left' },
  { name: 'type', label: t('type'), field: 'type', align: 'left' },
  { name: 'source', label: t('dataset.source.title'), field: 'source', align: 'left' },
  { name: 'name', label: t('name'), field: 'name', align: 'left', style: 'white-space: normal' },
  { name: 'description', label: t('description'), field: 'description', align: 'left', style: 'white-space: normal' },
  { name: 'actions', label: '', field: 'index', align: 'right' },
]);

// the selection is of the rows shown
watch(rows, () => {
  selected.value = [];
});

function save(list: HarmonizedTable[]) {
  emit('change', { ...props.dataset, protocol: withHarmonizedTables(props.dataset.protocol, list) });
}

function onSchemaSave(table: DatasetTable) {
  const protocol = withHarmonizedTables(props.dataset.protocol, tables.value);
  emit('change', {
    ...props.dataset,
    protocol: { ...protocol, harmonizationTable: table as DatasetDto_HarmonizationTableDto },
  });
}

function onEdit(index: number | undefined) {
  editedIndex.value = index;
  edited.value = index === undefined ? undefined : tables.value[index];
  showTable.value = true;
}

function onTableSave(table: DatasetTable, harmonization: boolean) {
  const list = [...tables.value];
  const item = { harmonization, table };
  if (editedIndex.value === undefined) list.push(item);
  else list[editedIndex.value] = item;
  save(list);
}

function onMove(index: number, delta: number) {
  const list = [...tables.value];
  list.splice(index + delta, 0, ...list.splice(index, 1));
  save(list);
}

function onRemove() {
  save(tables.value.filter((_, index) => !toRemove.value.includes(index)));
}
</script>
