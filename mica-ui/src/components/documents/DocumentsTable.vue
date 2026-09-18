<template>
  <div>
    <q-table
      flat
      :rows="documentsStore.listOf(target.type)"
      :columns="columns"
      :rows-per-page-options="ROWS_PER_PAGE"
      row-key="id"
      :loading="loading"
    >
      <template v-slot:body-cell-id="props">
        <q-td key="id" :props="props">
          <router-link :to="`${target.routeBase}/${props.value}`" class="text-primary">{{ props.value }}</router-link>
        </q-td>
      </template>
      <template v-slot:body-cell-name="props">
        <q-td key="name" :props="props">
          <div v-for="name in props.value" :key="name.lang">
            <q-badge color="primary" class="q-mr-sm">{{ name.lang }}</q-badge>
            {{ name.value }}
          </div>
        </q-td>
      </template>
      <template v-slot:body-cell-status="props">
        <q-td key="status" :props="props">
          <document-status-badge :state="props.value" />
        </q-td>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import type { TimestampsDto } from 'src/models/Mica';
import DocumentStatusBadge from 'src/components/documents/DocumentStatusBadge.vue';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import type { DocumentSummary } from 'src/stores/documents';
import { ROWS_PER_PAGE } from 'src/utils/constants';
import { getDateLabel } from 'src/utils/dates';
import { notifyError } from 'src/utils/notify';

interface Props {
  /** the target of the type, the id is not used */
  target: DocumentTarget;
}

const props = defineProps<Props>();
const documentsStore = useDocumentsStore();
const { t } = useI18n();

const loading = ref(false);

const columns = computed(() => [
  { name: 'id', label: 'ID', field: 'id', sortable: true },
  {
    name: 'name',
    label: t('name'),
    field: (row: DocumentSummary) => row.name ?? row.title ?? [],
    sortable: true,
  },
  { name: 'status', label: t('status'), field: 'state', sortable: false },
  {
    name: 'lastUpdated',
    label: t('last_modified'),
    field: 'timestamps',
    format: (ts: TimestampsDto | undefined) => getDateLabel(ts?.lastUpdate),
    sortable: true,
  },
]);

async function load() {
  loading.value = true;
  try {
    await documentsStore.fetchDocuments(props.target);
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

watch(() => props.target.type, load, { immediate: true });
</script>
