<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('persons.title')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-banner v-if="degraded" dense rounded class="bg-warning text-dark q-mb-sm">
        <template #avatar><q-icon name="warning" /></template>
        {{ t('persons.search_degraded') }}
      </q-banner>
      <q-table
        v-model:pagination="pagination"
        flat
        :rows="rows"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :filter="search"
        :rows-per-page-options="[10, 25, 50, 100]"
        :no-data-label="t('persons.none')"
        :no-results-label="t('persons.none')"
        binary-state-sort
        @request="onRequest"
      >
        <template v-slot:top-left>
          <div class="q-gutter-sm">
            <q-btn color="primary" icon="add" :label="t('persons.new')" size="sm" @click="showNew = true" />
            <q-btn
              outline
              color="primary"
              icon="download"
              :label="t('persons.download')"
              size="sm"
              :href="personsStore.downloadUrl(personsQuery(search), pagination.rowsNumber ?? 0)"
            />
            <q-btn
              outline
              color="primary"
              icon="cleaning_services"
              :label="t('persons.remove_duplicates')"
              size="sm"
              :loading="removing"
              @click="onRemoveDuplicates"
            />
          </div>
        </template>
        <template v-slot:top-right>
          <q-select
            v-model="field"
            :options="searchFields"
            dense
            options-dense
            emit-value
            map-options
            class="q-mr-sm"
          />
          <q-input
            v-model="filter"
            dense
            clearable
            debounce="300"
            :placeholder="field === 'all' ? t('search') : t('persons.search_id')"
          >
            <template v-slot:append>
              <q-icon name="search" />
            </template>
          </q-input>
        </template>
        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <router-link :to="`/persons/${props.row.id}`" class="text-primary">{{ fullName(props.row) }}</router-link>
          </q-td>
        </template>
        <template v-for="kind in MEMBERSHIP_KINDS" :key="kind" v-slot:[`body-cell-${kind}`]="props">
          <q-td :props="props">
            <div v-for="entity in groupMemberships(props.row, kind, locale)" :key="entity.id">
              <router-link :to="entity.route" class="text-primary">{{ entity.acronym }}</router-link>
              <span class="text-grey-7 q-ml-xs">({{ entity.roles.map(roleLabel).join(', ') }})</span>
            </div>
          </q-td>
        </template>
      </q-table>
    </q-page>
    <person-dialog v-model="showNew" @saved="(person) => router.push(`/persons/${person.id}`)" />
  </div>
</template>

<script setup lang="ts">
import { useQuasar, type QTableColumn, type QTableProps } from 'quasar';
import type { PersonDto, TimestampsDto } from 'src/models/Mica';
import PersonDialog from 'src/components/persons/PersonDialog.vue';
import { getDateLabel } from 'src/utils/dates';
import { notifyError, notifySuccess } from 'src/utils/notify';
import {
  fullName,
  groupMemberships,
  MEMBERSHIP_INFO,
  MEMBERSHIP_KINDS,
  fieldQuery,
  searchQuery,
} from 'src/utils/persons';
import { usePersonsStore } from 'src/stores/persons';
import { useRoleLabels } from 'src/composables/useRoleLabels';

type Pagination = NonNullable<QTableProps['pagination']>;

/** the search sort field of a column */
const SORT_FIELDS: Record<string, string> = { name: 'lastName', lastUpdate: 'lastModifiedDate' };

/** the search fields of the membership selector (individual studies and initiatives share the study memberships) */
const SEARCH_FIELDS: Record<string, string> = {
  all: 'all',
  study: 'studyMemberships.parentId',
  network: 'networkMemberships.parentId',
};

interface PersonsSearch {
  text: string;
  field: string;
}

/** the search query of the text, restricted to the membership field if any */
function personsQuery({ text, field }: PersonsSearch): string | undefined {
  const query = searchQuery(text);
  return query && fieldQuery(query, SEARCH_FIELDS[field] ?? 'all', false, '');
}

const personsStore = usePersonsStore();
const { roleLabel } = useRoleLabels();
const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { t, locale } = useI18n();

const loading = ref(false);
const showNew = ref(false);
const removing = ref(false);
const rows = ref<PersonDto[]>([]);
const degraded = ref(false);

// the search, the sort and the page are kept in the route query
const query = route.query;
const filter = ref(typeof query.q === 'string' ? query.q : '');
/** the membership field the search is restricted to, `all` for none */
const field = ref(typeof query.field === 'string' && query.field in SEARCH_FIELDS ? query.field : 'all');
const search = computed<PersonsSearch>(() => ({ text: filter.value ?? '', field: field.value }));
const searchFields = computed(() => [
  { value: 'all', label: t('documents.search_all') },
  { value: 'study', label: t('persons.search_study') },
  { value: 'network', label: t('persons.search_network') },
]);
const pagination = ref<Pagination>({
  sortBy: typeof query.sort === 'string' && query.sort in SORT_FIELDS ? query.sort : 'name',
  descending: query.order === 'desc',
  page: Number(query.page) || 1,
  rowsPerPage: Number(query.size) || 10,
  rowsNumber: 0,
});
/** the person just deleted, left out of the first search */
let exclude = typeof query.exclude === 'string' ? query.exclude : undefined;

const columns = computed<QTableColumn[]>(() => [
  { name: 'name', label: t('name'), field: 'lastName', align: 'left', sortable: true },
  { name: 'email', label: t('email'), field: 'email', align: 'left' },
  {
    name: 'lastUpdate',
    label: t('last_modified'),
    field: 'timestamps',
    format: (ts: TimestampsDto | undefined) => getDateLabel(ts?.lastUpdate),
    align: 'left',
    sortable: true,
  },
  ...MEMBERSHIP_KINDS.map(
    (kind): QTableColumn => ({
      name: kind,
      label: t(MEMBERSHIP_INFO[kind].title),
      field: MEMBERSHIP_INFO[kind].field,
      align: 'left',
    }),
  ),
]);

async function onRequest(props: { pagination: Pagination; filter?: unknown }) {
  const { page = 1, rowsPerPage = 10, sortBy, descending } = props.pagination;
  const { text, field } = props.filter as PersonsSearch;
  loading.value = true;
  try {
    const result = await personsStore.search({
      query: personsQuery({ text, field }),
      from: (page - 1) * rowsPerPage,
      limit: rowsPerPage,
      sort: SORT_FIELDS[sortBy ?? 'name'] ?? 'lastName',
      order: descending ? 'desc' : 'asc',
      exclude,
    });
    exclude = undefined;
    rows.value = result.persons;
    degraded.value = result.degraded;
    pagination.value = { ...props.pagination, rowsNumber: result.total };
    router.replace({
      query: {
        ...(text ? { q: text } : {}),
        ...(field !== 'all' ? { field } : {}),
        sort: sortBy ?? 'name',
        order: descending ? 'desc' : 'asc',
        page: String(page),
        size: String(rowsPerPage),
      },
    });
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

function onRemoveDuplicates() {
  $q.dialog({
    title: t('persons.remove_duplicates_title'),
    message: t('persons.remove_duplicates_text'),
    cancel: true,
    persistent: true,
  }).onOk(async () => {
    removing.value = true;
    try {
      const count = await personsStore.removeRedundants();
      notifySuccess(t('persons.duplicates_removed', { count }));
      await onRequest({ pagination: pagination.value, filter: search.value });
    } catch (error) {
      notifyError(error);
    } finally {
      removing.value = false;
    }
  });
}

onMounted(() => onRequest({ pagination: pagination.value, filter: search.value }));
</script>
