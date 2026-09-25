<template>
  <q-dialog v-model="showDialog" @hide="onHide">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('config.statistics.index_dialog.title', { type: typeTitle }) }}</div>
        <div class="text-hint">{{ t('config.statistics.index_dialog.info') }}</div>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pa-none">
        <q-table
          v-model:selected="selected"
          flat
          dense
          selection="multiple"
          :rows="items"
          :columns="columns"
          row-key="id"
          :loading="loading"
          :pagination="{ rowsPerPage: 10 }"
          :rows-per-page-options="[10, 25, 50]"
          :hide-pagination="items.length <= 10"
          :no-data-label="t('config.statistics.index_dialog.empty')"
        >
          <template v-slot:body-cell-id="props">
            <q-td :props="props">
              <router-link v-if="documentRoute" :to="`${documentRoute}/${props.value}`" class="text-primary">
                {{ props.value }}
              </router-link>
              <span v-else>{{ props.value }}</span>
            </q-td>
          </template>
        </q-table>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn
          :label="`${t('config.statistics.index_dialog.index')} (${selected.length})`"
          color="primary"
          :disable="selected.length === 0"
          :loading="indexing"
          @click="onIndex"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import { DOCUMENT_METRICS_TYPES, type MetricsType } from 'src/composables/useContentMetrics';
import { documentTarget } from 'src/composables/useDocumentTarget';
import type { EntityIndexHealthDto_ItemDto } from 'src/models/Mica';

/**
 * The published documents of a type missing from the search index, to index (all selected by
 * default).
 */
const props = defineProps<{
  modelValue: boolean;
  type: MetricsType | undefined;
  /** loads the documents requiring indexing */
  load: (type: MetricsType) => Promise<EntityIndexHealthDto_ItemDto[]>;
  /** indexes the selected documents */
  index: (type: MetricsType, ids: string[]) => Promise<boolean>;
}>();

const emit = defineEmits(['update:modelValue', 'indexed']);

const { t, locale } = useI18n();

const showDialog = ref(props.modelValue);
const loading = ref(false);
const indexing = ref(false);
const items = ref<EntityIndexHealthDto_ItemDto[]>([]);
const selected = ref<EntityIndexHealthDto_ItemDto[]>([]);

const typeTitle = computed(() => (props.type ? t(`config.statistics.types.${props.type}`) : ''));
const documentRoute = computed(() => {
  const documentType = props.type ? DOCUMENT_METRICS_TYPES[props.type] : undefined;
  return documentType ? documentTarget(documentType, '').routeBase : undefined;
});

const columns = computed<QTableColumn<EntityIndexHealthDto_ItemDto>[]>(() => [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
  {
    name: 'title',
    label: t('name'),
    field: (row) => localizedTitle(row),
    sortable: true,
    align: 'left',
    style: 'white-space: normal',
  },
]);

/** the title in the current locale, or the first one */
function localizedTitle(item: EntityIndexHealthDto_ItemDto): string {
  const titles = item.title ?? [];
  return titles.find((entry) => entry.lang === locale.value)?.value ?? titles[0]?.value ?? '';
}

/** identifies the latest load, so that a stale response cannot fill a dialog opened for another type */
let loadSequence = 0;

watch(
  () => props.modelValue,
  async (value) => {
    showDialog.value = value;
    const sequence = ++loadSequence;
    if (value && props.type) {
      loading.value = true;
      items.value = [];
      selected.value = [];
      try {
        const loaded = await props.load(props.type);
        if (sequence !== loadSequence) return;
        items.value = loaded;
        selected.value = loaded.slice();
      } finally {
        if (sequence === loadSequence) loading.value = false;
      }
    }
  },
);

function onHide() {
  emit('update:modelValue', false);
}

async function onIndex() {
  if (!props.type) return;
  indexing.value = true;
  try {
    if (
      await props.index(
        props.type,
        selected.value.map((item) => item.id),
      )
    ) {
      emit('indexed', selected.value.length);
      showDialog.value = false;
    }
  } finally {
    indexing.value = false;
  }
}
</script>
