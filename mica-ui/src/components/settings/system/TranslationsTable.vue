<template>
  <div>
    <div class="row items-center q-col-gutter-md q-mb-md">
      <div class="col-12 col-md-4">
        <q-input
          v-model="filter"
          dense
          outlined
          clearable
          debounce="300"
          :placeholder="t('config.translations.filter')"
        >
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
        </q-input>
      </div>
      <div class="col-auto">
        <q-toggle v-model="customizedOnly" dense :label="t('config.translations.customized_only')" />
      </div>
      <div class="col-auto q-gutter-sm">
        <q-btn
          outline
          no-caps
          color="primary"
          icon="add"
          size="sm"
          :label="t('config.translations.add')"
          @click="onAdd"
        />
        <q-btn-dropdown
          outline
          no-caps
          color="grey-8"
          icon="download"
          size="sm"
          :label="t('config.translations.export')"
        >
          <q-list>
            <q-item clickable v-close-popup @click="onExportJson">
              <q-item-section>
                <q-item-label>{{ t('config.translations.export_json') }}</q-item-label>
                <q-item-label caption>{{ t('config.translations.export_json_help') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item v-for="locale in languages" :key="locale" clickable v-close-popup @click="onExportGettext(locale)">
              <q-item-section>
                <q-item-label>{{
                  t('config.translations.export_gettext', { locale: locale.toUpperCase() })
                }}</q-item-label>
                <q-item-label caption>{{ t('config.translations.export_gettext_help') }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
        <q-btn
          outline
          no-caps
          color="grey-8"
          icon="upload"
          size="sm"
          :label="t('config.translations.import')"
          :disable="dirty"
          @click="fileInput?.click()"
        />
        <input ref="fileInput" type="file" accept=".json,application/json" style="display: none" @change="onImport" />
      </div>
      <q-space />
      <div class="col-auto">
        <q-btn
          color="primary"
          icon="save"
          size="sm"
          :label="t('save')"
          :disable="!dirty"
          :loading="saving"
          @click="onSave"
        />
      </div>
    </div>
    <q-resize-observer :debounce="200" @resize="(size) => (tableWidth = size.width)" />
    <q-table
      flat
      bordered
      dense
      wrap-cells
      class="translations-table"
      :rows="rows"
      :columns="columns"
      row-key="key"
      :loading="loading"
      v-model:pagination="pagination"
      :sort-method="sortRows"
      :rows-per-page-options="[25, 50, 100]"
      :no-data-label="t('config.translations.none')"
    >
      <template v-slot:header-cell="props">
        <q-th :props="props">
          {{ props.col.label }}
          <q-badge v-if="customizedCounts[props.col.name]" color="primary" rounded class="q-ml-xs">
            {{ customizedCounts[props.col.name] }}
          </q-badge>
        </q-th>
      </template>
      <template v-slot:body="props">
        <q-tr :props="props">
          <q-td key="key" :props="props" class="translation-key">
            <code>{{ props.row.key }}</code>
            <q-btn
              v-if="!hasDefault(props.row.key)"
              flat
              round
              dense
              size="sm"
              color="negative"
              icon="delete"
              :title="t('delete')"
              @click="removeKey(props.row.key)"
            />
          </q-td>
          <q-td
            v-for="locale in languages"
            :key="locale"
            :props="props"
            :class="{ 'bg-blue-1': isCustomized(props.row.key, locale) }"
          >
            <q-input
              :key="tableWidth"
              :model-value="value(props.row.key, locale)"
              dense
              borderless
              autogrow
              @update:model-value="(v) => setValue(props.row.key, locale, String(v ?? ''))"
            >
              <template v-if="isCustomized(props.row.key, locale) && hasDefault(props.row.key)" v-slot:append>
                <q-btn
                  flat
                  round
                  dense
                  size="sm"
                  icon="undo"
                  :title="t('config.translations.reset')"
                  @click="reset(props.row.key, locale)"
                />
              </template>
            </q-input>
          </q-td>
        </q-tr>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import { exportFile, useQuasar, type QTableColumn } from 'quasar';
import { useTranslations } from 'src/composables/useTranslations';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import { notifyError, notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const $q = useQuasar();
const systemStore = useSystemStore();
const {
  languages,
  loading,
  saving,
  dirty,
  customizedCounts,
  hasDefault,
  isCustomized,
  value,
  filterKeys,
  setValue,
  reset,
  addKey,
  removeKey,
  load,
  save,
  exportAll,
  exportGettext,
  importAll,
} = useTranslations();
useConfirmLeave(dirty);

const filter = ref<string | null>('');
const customizedOnly = ref(false);
const fileInput = ref<HTMLInputElement>();
/** the autogrow inputs measure their height only when mounted or edited: remounted when the width changes */
const tableWidth = ref(0);

const pagination = ref<{ rowsPerPage: number; sortBy?: string | null; descending?: boolean }>({ rowsPerPage: 50 });

type Row = { key: string };

const rows = computed<Row[]>(() => filterKeys(filter.value ?? '', customizedOnly.value).map((key) => ({ key })));

const columns = computed<QTableColumn<Row>[]>(() => [
  {
    name: 'key',
    label: t('config.translations.key'),
    field: 'key',
    align: 'left',
    sortable: true,
    style: 'width: 30%',
  },
  ...languages.value.map((locale) => ({
    name: locale,
    label: locale.toUpperCase(),
    field: (row: Row) => value(row.key, locale),
    align: 'left' as const,
    sortable: true,
  })),
]);

/**
 * The values of the sorted language, as they were when the sort, the filter or the translations last changed:
 * editing a value does not move its row away while typing.
 */
const sortValues = shallowRef(new Map<string, string>());
watch(
  [() => pagination.value.sortBy, filter, customizedOnly, loading],
  ([sortBy]) => {
    const locale = sortBy && sortBy !== 'key' ? sortBy : undefined;
    sortValues.value = new Map(locale ? filterKeys('').map((key) => [key, value(key, locale)]) : []);
  },
  { immediate: true },
);

function sortRows(data: readonly Row[], sortBy: string, descending: boolean): Row[] {
  const values = sortValues.value;
  const text = (row: Row) => (sortBy === 'key' ? row.key : (values.get(row.key) ?? ''));
  const sign = descending ? -1 : 1;
  return [...data].sort((a, b) => sign * text(a).localeCompare(text(b)) || a.key.localeCompare(b.key));
}

function onAdd() {
  $q.dialog({
    title: t('config.translations.add'),
    message: t('config.translations.add_hint'),
    prompt: { model: '', type: 'text' },
    cancel: true,
  }).onOk((key: string) => {
    if (addKey(key)) {
      filter.value = key.trim();
      customizedOnly.value = false;
    } else notifyError('config.translations.invalid_key');
  });
}

async function onSave() {
  if (await save()) notifySuccess('config.translations.saved');
}

async function onExportJson() {
  const data = await exportAll();
  if (data) exportFile('mica-all.json', JSON.stringify(data, null, 2), 'application/json');
}

async function onExportGettext(locale: string) {
  const data = await exportGettext(locale);
  if (data !== undefined) exportFile(`mica-${locale}.po`, data, 'text/plain');
}

async function onImport(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  const text = await file.text();
  $q.dialog({
    title: t('config.translations.import'),
    message: t('config.translations.import_text', { name: file.name }),
    options: { type: 'checkbox', model: ['merge'], items: [{ label: t('config.translations.merge'), value: 'merge' }] },
    cancel: true,
  }).onOk(async (options: string[]) => {
    if (await importAll(text, options.includes('merge'))) notifySuccess('config.translations.imported');
  });
}

onMounted(async () => {
  await systemStore.init();
  await load(systemStore.languages);
});
</script>

<style scoped>
/* fixed column widths, not depending on the texts */
.translations-table :deep(table) {
  table-layout: fixed;
}
.translation-key code {
  font-size: 0.8rem;
  word-break: break-all;
}
</style>
