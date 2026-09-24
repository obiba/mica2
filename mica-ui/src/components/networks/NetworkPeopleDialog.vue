<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ t('network_members.associated_people') }}</div>
        <div class="text-hint">{{ t('network_members.associated_people_help') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll">
        <q-table
          flat
          dense
          :rows="persons"
          :columns="columns"
          row-key="id"
          :loading="loading"
          :filter="filter"
          :pagination="{ rowsPerPage: 10 }"
          :rows-per-page-options="[10, 25, 50]"
          :no-data-label="t('persons.none')"
        >
          <template v-slot:top-right>
            <q-input v-model="filter" dense debounce="300" :placeholder="t('search')">
              <template v-slot:append>
                <q-icon name="search" />
              </template>
            </q-input>
          </template>
          <template v-slot:body-cell-name="props">
            <q-td :props="props">
              <router-link :to="`/persons/${props.row.id}`" class="text-primary">{{ props.value }}</router-link>
            </q-td>
          </template>
          <template v-for="kind in MEMBERSHIP_KINDS" :key="kind" v-slot:[`body-cell-${kind}`]="props">
            <q-td :props="props" style="white-space: normal">
              <span v-for="(entity, index) in groupMemberships(props.row, kind, locale)" :key="entity.id">
                <router-link :to="entity.route" class="text-primary">{{ entity.acronym }}</router-link>
                <em class="text-grey-7"> ({{ entity.roles.map(roleLabel).join(', ') }})</em
                ><span v-if="index < groupMemberships(props.row, kind, locale).length - 1">, </span>
              </span>
            </q-td>
          </template>
        </q-table>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn
          v-if="query && persons.length > 0"
          flat
          icon="download"
          :label="t('persons.download')"
          color="primary"
          :href="personsStore.downloadUrl(query, total)"
          download
        />
        <q-btn flat :label="t('close')" color="secondary" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { NetworkDto, PersonDto } from 'src/models/Mica';
import { useRoleLabels } from 'src/composables/useRoleLabels';
import { usePersonsStore } from 'src/stores/persons';
import { associatedPeopleQuery } from 'src/utils/networks';
import { notifyError } from 'src/utils/notify';
import { fullName, groupMemberships, MEMBERSHIP_INFO, MEMBERSHIP_KINDS } from 'src/utils/persons';

interface Props {
  network: NetworkDto;
}

/** as many as the legacy admin app */
const LIMIT = 999;

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const { t, locale } = useI18n();
const personsStore = usePersonsStore();
const { roleLabel } = useRoleLabels();

const columns = computed<QTableColumn[]>(() => [
  { name: 'name', label: t('name'), field: (row: PersonDto) => fullName(row), align: 'left', sortable: true },
  { name: 'email', label: t('email'), field: 'email', align: 'left', sortable: true },
  ...MEMBERSHIP_KINDS.map(
    (kind): QTableColumn => ({
      name: kind,
      label: t(MEMBERSHIP_INFO[kind].title),
      field: (row: PersonDto) =>
        groupMemberships(row, kind, locale.value)
          .map((entity) => entity.acronym)
          .join(' '),
      align: 'left',
    }),
  ),
]);

const loading = ref(false);
const persons = ref<PersonDto[]>([]);
const total = ref(0);
const filter = ref('');
const query = computed(() => associatedPeopleQuery(props.network));

async function onShow() {
  filter.value = '';
  persons.value = [];
  total.value = 0;
  if (!query.value) return;
  loading.value = true;
  try {
    const result = await personsStore.search({
      query: query.value,
      from: 0,
      limit: LIMIT,
      sort: 'lastName',
      order: 'asc',
    });
    persons.value = result.persons;
    total.value = result.total;
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
}
</script>
