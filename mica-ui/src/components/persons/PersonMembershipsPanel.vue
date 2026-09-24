<template>
  <div>
    <div class="row items-center q-mb-xs">
      <div class="text-subtitle1 col">{{ t(MEMBERSHIP_INFO[kind].title) }}</div>
      <q-btn
        color="primary"
        icon="add"
        :label="t('persons.add_memberships')"
        size="sm"
        :disable="busy"
        @click="showAdd = true"
      />
    </div>
    <div class="text-hint q-mb-sm">{{ t(`persons.memberships_help.${kind}`) }}</div>
    <q-table
      flat
      dense
      :rows="entities"
      :columns="columns"
      row-key="id"
      :pagination="{ rowsPerPage: 10 }"
      :rows-per-page-options="[10, 25, 50]"
      :no-data-label="t('persons.no_entity_memberships')"
      :hide-pagination="entities.length <= 10"
    >
      <template v-slot:body-cell-acronym="props">
        <q-td :props="props">
          <router-link :to="props.row.route" class="text-primary">{{ props.value }}</router-link>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props" class="text-no-wrap">
          <q-btn
            flat
            dense
            size="sm"
            icon="edit"
            color="primary"
            :title="t('edit')"
            :disable="busy"
            @click="onEditRoles(props.row)"
          />
          <q-btn
            flat
            dense
            size="sm"
            icon="delete"
            color="negative"
            :title="t('delete')"
            :disable="busy"
            @click="toRemove = props.row"
          />
        </q-td>
      </template>
    </q-table>

    <add-memberships-dialog v-model="showAdd" :kind="kind" :person="person" @add="onAdd" />

    <q-dialog v-model="showRoles">
      <q-card class="dialog-sm">
        <q-card-section>
          <div class="text-h6">{{ t('persons.edit_roles_title', { acronym: toEdit?.acronym }) }}</div>
        </q-card-section>
        <q-separator />
        <q-card-section>
          <q-option-group v-model="roles" :options="roleOptions" type="checkbox" />
        </q-card-section>
        <q-separator />
        <q-card-actions align="right" class="bg-grey-3">
          <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
          <q-btn
            flat
            :label="t('save')"
            color="primary"
            :disable="roles.length === 0"
            v-close-popup
            @click="onSaveRoles"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <confirm-dialog
      :model-value="toRemove !== undefined"
      :title="t('persons.remove_membership_title')"
      :text="t('persons.remove_membership_text', { name: fullName(person), acronym: toRemove?.acronym })"
      @update:model-value="toRemove = undefined"
      @confirm="onRemove"
    />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { PersonDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import AddMembershipsDialog from 'src/components/persons/AddMembershipsDialog.vue';
import {
  fullName,
  groupMemberships,
  MEMBERSHIP_INFO,
  withMemberships,
  type MembershipEntity,
  type MembershipKind,
  type MembershipParent,
} from 'src/utils/persons';

interface Props {
  person: PersonDto;
  kind: MembershipKind;
  /** a change is being saved */
  busy?: boolean;
}

const props = defineProps<Props>();
/** the person with the memberships changed, to be saved */
const emit = defineEmits<{ change: [person: PersonDto] }>();
const { t, locale } = useI18n();
const systemStore = useSystemStore();

const entities = computed(() => groupMemberships(props.person, props.kind, locale.value));
const roleOptions = computed(() =>
  (systemStore.configuration.roles ?? []).map((role) => ({ label: role, value: role })),
);

const columns = computed<QTableColumn[]>(() => [
  { name: 'acronym', label: t('acronym'), field: 'acronym', align: 'left', sortable: true },
  { name: 'name', label: t('name'), field: 'name', align: 'left', sortable: true, style: 'white-space: normal' },
  {
    name: 'roles',
    label: t('persons.roles'),
    field: 'roles',
    align: 'left',
    format: (roles: string[]) => roles.join(', '),
  },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]);

const showAdd = ref(false);
const showRoles = ref(false);
const toEdit = ref<MembershipEntity>();
const roles = ref<string[]>([]);
const toRemove = ref<MembershipEntity>();

/** the entity as a membership parent: its localized acronym and name are kept as they are */
function parentOf(entity: MembershipEntity): MembershipParent {
  const membership = props.person[MEMBERSHIP_INFO[props.kind].field].find((item) => item.parentId === entity.id);
  return { id: entity.id, acronym: membership?.parentAcronym ?? [], name: membership?.parentName ?? [] };
}

function onAdd(parents: MembershipParent[], selectedRoles: string[]) {
  emit('change', withMemberships(props.person, props.kind, parents, selectedRoles));
}

function onEditRoles(entity: MembershipEntity) {
  toEdit.value = entity;
  roles.value = [...entity.roles];
  showRoles.value = true;
}

function onSaveRoles() {
  if (toEdit.value) emit('change', withMemberships(props.person, props.kind, [parentOf(toEdit.value)], roles.value));
}

function onRemove() {
  if (toRemove.value) emit('change', withMemberships(props.person, props.kind, [parentOf(toRemove.value)], []));
}
</script>
