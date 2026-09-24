<template>
  <div>
    <div class="row items-center q-gutter-sm q-mb-md">
      <q-btn
        v-if="peopleQuery"
        color="secondary"
        icon="groups"
        :label="t('network_members.associated_people')"
        size="sm"
        @click="showPeople = true"
      />
    </div>
    <q-spinner-dots v-if="loading" color="primary" size="2em" />
    <div v-else class="row q-col-gutter-md">
      <div v-for="item in members" :key="item.role" class="col-12 col-md-6">
        <q-card flat bordered>
          <q-card-section class="row items-center q-py-sm">
            <div class="text-subtitle1 col">{{ item.role }}</div>
            <q-btn
              v-if="canEdit"
              flat
              dense
              size="sm"
              icon="add"
              color="primary"
              :label="t('add')"
              :disable="working"
              @click="onShowAdd(item)"
            />
          </q-card-section>
          <q-separator />
          <q-list dense separator>
            <q-item v-for="(person, index) in item.persons" :key="person.id ?? index">
              <q-item-section>
                <q-item-label>
                  <router-link :to="`/persons/${person.id}`" class="text-primary">{{ fullName(person) }}</router-link>
                </q-item-label>
                <q-item-label v-if="person.institution" caption>
                  {{ localized(person.institution.name, locale) }}
                </q-item-label>
              </q-item-section>
              <q-item-section v-if="canEdit" side class="row no-wrap">
                <div class="text-no-wrap">
                  <q-btn
                    flat
                    dense
                    size="sm"
                    icon="arrow_upward"
                    :title="t('network_members.move_up')"
                    :disable="working || index === 0"
                    @click="onMove(item, person, -1)"
                  />
                  <q-btn
                    flat
                    dense
                    size="sm"
                    icon="arrow_downward"
                    :title="t('network_members.move_down')"
                    :disable="working || index === item.persons.length - 1"
                    @click="onMove(item, person, 1)"
                  />
                  <q-btn
                    flat
                    dense
                    size="sm"
                    icon="delete"
                    color="negative"
                    :title="t('delete')"
                    :disable="working"
                    @click="toRemove = { role: item.role, person }"
                  />
                </div>
              </q-item-section>
            </q-item>
            <q-item v-if="item.persons.length === 0">
              <q-item-section class="text-hint">{{ t('network_members.none') }}</q-item-section>
            </q-item>
          </q-list>
        </q-card>
      </div>
    </div>

    <add-member-dialog
      v-model="showAdd"
      :role="addRole?.role ?? ''"
      :exclude="(addRole?.persons ?? []).map((person) => person.id ?? '')"
      @add="onAdd"
    />
    <network-people-dialog v-model="showPeople" :network="network" />

    <confirm-dialog
      :model-value="toRemove !== undefined"
      :title="t('network_members.remove_title')"
      :text="t('network_members.remove_text', { name: fullName(toRemove?.person), role: toRemove?.role })"
      @update:model-value="toRemove = undefined"
      @confirm="onRemove"
    />
  </div>
</template>

<script setup lang="ts">
import type { NetworkDto, PersonDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import AddMemberDialog from 'src/components/networks/AddMemberDialog.vue';
import NetworkPeopleDialog from 'src/components/networks/NetworkPeopleDialog.vue';
import { usePersonsStore } from 'src/stores/persons';
import { notifyError } from 'src/utils/notify';
import { associatedPeopleQuery, membersByRole, moveMember, type RoleMembers } from 'src/utils/networks';
import { fullName, localized, withMemberships } from 'src/utils/persons';

interface Props {
  network: NetworkDto;
  canEdit: boolean;
  /** a change of the network is being saved */
  busy?: boolean;
}

const props = defineProps<Props>();
/** the network with the members order changed, to be saved */
const emit = defineEmits<{ change: [network: NetworkDto] }>();
const { t, locale } = useI18n();
const personsStore = usePersonsStore();
const systemStore = useSystemStore();

const loading = ref(false);
const saving = ref(false);
const working = computed(() => props.busy || saving.value);
const persons = ref<PersonDto[]>([]);
const networkId = computed(() => props.network.id ?? '');
const members = computed(() =>
  membersByRole(
    persons.value,
    networkId.value,
    systemStore.configuration.roles ?? [],
    props.network.membershipSortOrder,
  ),
);
const peopleQuery = computed(() => associatedPeopleQuery(props.network));

const showAdd = ref(false);
const addRole = ref<RoleMembers>();
const showPeople = ref(false);
const toRemove = ref<{ role: string; person: PersonDto }>();

async function load() {
  loading.value = true;
  try {
    persons.value = await personsStore.fetchNetworkMembers(networkId.value);
  } catch (error) {
    persons.value = [];
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

/** the roles of the person in the network */
function rolesOf(person: PersonDto): string[] {
  return (person.networkMemberships ?? [])
    .filter((membership) => membership.parentId === networkId.value)
    .map((membership) => membership.role);
}

/** saves the person with the given roles in the network, then reloads the members */
async function saveRoles(person: PersonDto, roles: string[]) {
  const parent = { id: networkId.value, acronym: props.network.acronym, name: props.network.name };
  saving.value = true;
  try {
    await personsStore.update(withMemberships(person, 'network', [parent], roles));
    await load();
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}

function onShowAdd(item: RoleMembers) {
  addRole.value = item;
  showAdd.value = true;
}

async function onAdd(person: PersonDto) {
  const role = addRole.value?.role;
  if (!role || !person.id) return;
  try {
    // the search gives the indexed person, the draft one is saved
    const draft = await personsStore.get(person.id);
    await saveRoles(draft, [...new Set([...rolesOf(draft), role])]);
  } catch (error) {
    notifyError(error);
  }
}

async function onRemove() {
  const removed = toRemove.value;
  if (removed)
    await saveRoles(
      removed.person,
      rolesOf(removed.person).filter((role) => role !== removed.role),
    );
}

function onMove(item: RoleMembers, person: PersonDto, delta: number) {
  emit('change', {
    ...props.network,
    membershipSortOrder: moveMember(members.value, item.role, person.id ?? '', delta),
  });
}

watch(networkId, load, { immediate: true });
</script>
