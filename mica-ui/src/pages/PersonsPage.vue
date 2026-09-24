<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('persons.title')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-table
        v-model:pagination="pagination"
        flat
        :rows="rows"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :filter="filter"
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
              :href="personsStore.downloadUrl(searchQuery(filter), pagination.rowsNumber ?? 0)"
            />
          </div>
        </template>
        <template v-slot:top-right>
          <q-input v-model="filter" dense clearable debounce="300" :placeholder="t('search')">
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
              <span class="text-grey-7 q-ml-xs">({{ entity.roles.join(', ') }})</span>
            </div>
          </q-td>
        </template>
      </q-table>
    </q-page>
    <person-dialog v-model="showNew" @saved="(person) => router.push(`/persons/${person.id}`)" />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn, QTableProps } from 'quasar';
import type { PersonDto, TimestampsDto } from 'src/models/Mica';
import PersonDialog from 'src/components/persons/PersonDialog.vue';
import { getDateLabel } from 'src/utils/dates';
import { notifyError } from 'src/utils/notify';
import { fullName, groupMemberships, MEMBERSHIP_INFO, MEMBERSHIP_KINDS, searchQuery } from 'src/utils/persons';
import { usePersonsStore } from 'src/stores/persons';

type Pagination = NonNullable<QTableProps['pagination']>;

/** the search sort field of a column */
const SORT_FIELDS: Record<string, string> = { name: 'lastName', lastUpdate: 'lastModifiedDate' };

const personsStore = usePersonsStore();
const route = useRoute();
const router = useRouter();
const { t, locale } = useI18n();

const loading = ref(false);
const showNew = ref(false);
const rows = ref<PersonDto[]>([]);

// the search, the sort and the page are kept in the route query
const query = route.query;
const filter = ref(typeof query.q === 'string' ? query.q : '');
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
  const text = typeof props.filter === 'string' ? props.filter : '';
  loading.value = true;
  try {
    const result = await personsStore.search({
      query: searchQuery(text),
      from: (page - 1) * rowsPerPage,
      limit: rowsPerPage,
      sort: SORT_FIELDS[sortBy ?? 'name'] ?? 'lastName',
      order: descending ? 'desc' : 'asc',
      exclude,
    });
    exclude = undefined;
    rows.value = result.persons;
    pagination.value = { ...props.pagination, rowsNumber: result.total };
    router.replace({
      query: {
        ...(text ? { q: text } : {}),
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

onMounted(() => onRequest({ pagination: pagination.value, filter: filter.value }));
</script>
