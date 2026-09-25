<template>
  <div>
    <div class="row items-center q-gutter-xs q-mb-sm">
      <q-chip
        clickable
        :outline="statusFilter !== undefined"
        color="grey-7"
        text-color="white"
        @click="setStatusFilter(undefined)"
      >
        {{ t('documents.filter.ALL') }}
      </q-chip>
      <q-chip
        v-for="filter in STATE_FILTERS"
        :key="filter"
        clickable
        :outline="statusFilter !== filter"
        color="primary"
        :text-color="statusFilter === filter ? 'white' : 'primary'"
        @click="setStatusFilter(filter)"
      >
        {{ t(`documents.filter.${filter}`) }}
      </q-chip>
      <q-space />
      <q-select
        :model-value="searchField.value"
        :options="searchFields"
        dense
        options-dense
        emit-value
        map-options
        @update:model-value="setSearchField"
      />
      <q-input
        :model-value="searchText"
        dense
        clearable
        debounce="500"
        :placeholder="t('search')"
        @update:model-value="setSearchText"
      >
        <template v-slot:append>
          <q-icon name="search" />
        </template>
      </q-input>
    </div>
    <q-table
      flat
      :rows="rows"
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
      <template v-slot:body-cell-request="props">
        <q-td key="request" :props="props">
          <data-access-request-link :request="props.value" />
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
import DataAccessRequestLink from 'src/components/projects/DataAccessRequestLink.vue';
import { isStateFilter, matchesStateFilter, STATE_FILTERS, type StateFilter } from 'src/composables/useDocumentState';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import type { DocumentSummary } from 'src/stores/documents';
import { ROWS_PER_PAGE } from 'src/utils/constants';
import { getDateLabel } from 'src/utils/dates';
import { notifyError } from 'src/utils/notify';
import { fieldQuery, searchQuery } from 'src/utils/persons';

interface Props {
  /** the target of the type, the id is not used */
  target: DocumentTarget;
}

const props = defineProps<Props>();
const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();

const loading = ref(false);

/** the state filter, carried by the `status` route query so that the statistics summary can link here */
const statusFilter = computed<StateFilter | undefined>(() =>
  isStateFilter(route.query.status) ? route.query.status : undefined,
);

const rows = computed(() =>
  documentsStore.listOf(props.target.type).filter((row) => matchesStateFilter(row.state, statusFilter.value)),
);

function setStatusFilter(filter: StateFilter | undefined) {
  const query = { ...route.query };
  if (filter) query.status = filter;
  else delete query.status;
  router.replace({ query });
}

/** the free text search, carried by the `q` route query, searched by the server */
const searchText = computed(() => (typeof route.query.q === 'string' ? route.query.q : ''));

function setSearchText(text: string | number | null) {
  const query = { ...route.query };
  if (text) query.q = String(text);
  else delete query.q;
  router.replace({ query });
}

/** the fields the search can be restricted to, the projects have a title instead of an acronym and a name */
const searchFields = computed(() => [
  { value: 'all', label: t('documents.search_all'), localized: false },
  { value: 'id', label: 'ID', localized: false },
  ...(props.target.type === 'project'
    ? [{ value: 'title', label: t('documents.search_title'), localized: true }]
    : [
        { value: 'acronym', label: t('acronym'), localized: true },
        { value: 'name', label: t('name'), localized: true },
      ]),
]);

/** the field the search is restricted to, carried by the `field` route query */
const searchField = computed(
  () => searchFields.value.find((field) => field.value === route.query.field) ?? searchFields.value[0]!,
);

function setSearchField(field: string) {
  const query = { ...route.query };
  if (field !== 'all') query.field = field;
  else delete query.field;
  router.replace({ query });
}

/** the search query of the text restricted to the field, the localized fields in the UI language if configured */
function documentsQuery(): string | undefined {
  const query = searchQuery(searchText.value);
  if (!query) return undefined;
  const lang = systemStore.languages.includes(locale.value) ? locale.value : 'en';
  return fieldQuery(query, searchField.value.value, searchField.value.localized, lang);
}

const columns = computed(() => [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' as const },
  {
    name: 'name',
    label: props.target.type === 'project' ? t('documents.search_title') : t('name'),
    field: (row: DocumentSummary) => row.name ?? row.title ?? [],
    sortable: true,
    align: 'left' as const,
    // the titles (projects) can be long
    style: 'white-space: normal',
  },
  // the research projects may come from a data access request
  ...(props.target.type === 'project'
    ? [
        {
          name: 'request',
          label: t('data_access_request.title'),
          field: 'request',
          sortable: false,
          align: 'left' as const,
        },
      ]
    : []),
  { name: 'status', label: t('status'), field: 'state', sortable: false, align: 'left' as const },
  {
    name: 'lastUpdated',
    label: t('last_modified'),
    field: 'timestamps',
    format: (ts: TimestampsDto | undefined) => getDateLabel(ts?.lastUpdate),
    sortable: true,
    align: 'left' as const,
  },
]);

async function load() {
  loading.value = true;
  try {
    await documentsStore.fetchDocuments(props.target, documentsQuery());
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

watch([() => props.target.type, searchText, () => searchField.value.value], load, { immediate: true });
</script>
