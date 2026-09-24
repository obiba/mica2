<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('persons.add_memberships_title') }}: {{ t(MEMBERSHIP_INFO[kind].title) }}</div>
        <div class="text-hint">{{ t('persons.add_memberships_help') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-subtitle2">{{ t('persons.roles') }}</div>
        <q-option-group v-model="roles" :options="roleOptions" type="checkbox" inline />
      </q-card-section>
      <q-card-section class="q-pt-none">
        <q-table
          v-model:selected="selected"
          flat
          dense
          :rows="rows"
          :columns="columns"
          row-key="id"
          selection="multiple"
          :loading="loading"
          :filter="filter"
          :pagination="{ rowsPerPage: 10 }"
          :rows-per-page-options="[10, 25, 50]"
          :no-data-label="t('persons.member_of_all', { name: fullName(person) })"
        >
          <template v-slot:top-right>
            <q-input v-model="filter" dense debounce="300" :placeholder="t('search')">
              <template v-slot:append>
                <q-icon name="search" />
              </template>
            </q-input>
          </template>
        </q-table>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn
          flat
          :label="t('add')"
          color="primary"
          :disable="roles.length === 0 || selected.length === 0"
          v-close-popup
          @click="onAdd"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { LocalizedStringDto, PersonDto } from 'src/models/Mica';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { notifyError } from 'src/utils/notify';
import {
  fullName,
  groupMemberships,
  localized,
  MEMBERSHIP_INFO,
  type MembershipKind,
  type MembershipParent,
} from 'src/utils/persons';

interface Props {
  person: PersonDto;
  kind: MembershipKind;
}

/** an entity the person can become member of */
interface Row extends MembershipParent {
  acronymLabel: string;
  nameLabel: string;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ add: [parents: MembershipParent[], roles: string[]] }>();
const { t, locale } = useI18n();
const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();

const roleOptions = computed(() =>
  (systemStore.configuration.roles ?? []).map((role) => ({ label: role, value: role })),
);
const columns = computed<QTableColumn[]>(() => [
  { name: 'acronym', label: t('acronym'), field: 'acronymLabel', align: 'left', sortable: true },
  { name: 'name', label: t('name'), field: 'nameLabel', align: 'left', sortable: true, style: 'white-space: normal' },
]);

const loading = ref(false);
const rows = ref<Row[]>([]);
const selected = ref<Row[]>([]);
const roles = ref<string[]>([]);
const filter = ref('');

/** the entities of the kind the person is not member of yet */
async function onShow() {
  selected.value = [];
  roles.value = [];
  filter.value = '';
  loading.value = true;
  try {
    const members = new Set(groupMemberships(props.person, props.kind, locale.value).map((entity) => entity.id));
    const list = await documentsStore.fetchDocuments(documentTarget(MEMBERSHIP_INFO[props.kind].documentType, ''));
    rows.value = list
      .filter((entity) => entity.id && !members.has(entity.id))
      .map((entity) => {
        const acronym = (entity as { acronym?: LocalizedStringDto[] }).acronym ?? [];
        const name = entity.name ?? [];
        return {
          id: entity.id as string,
          acronym,
          name,
          acronymLabel: localized(acronym, locale.value),
          nameLabel: localized(name, locale.value),
        };
      })
      .sort((a, b) => a.acronymLabel.localeCompare(b.acronymLabel));
  } catch (error) {
    rows.value = [];
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

function onAdd() {
  emit(
    'add',
    selected.value.map(({ id, acronym, name }) => ({ id, acronym, name })),
    roles.value,
  );
}
</script>
