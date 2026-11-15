<template>
  <div>
    <q-page class="q-pa-none column">
      <q-toolbar class="bg-grey-3">
        <q-breadcrumbs>
          <q-breadcrumbs-el icon="home" to="/" />
          <template v-if="origin && originTarget">
            <q-breadcrumbs-el :label="t(originTarget.labels.title)" :to="originTarget.listRoute" />
            <q-breadcrumbs-el :label="origin.id" :to="origin.path" />
          </template>
          <q-breadcrumbs-el v-else :label="t('persons.title')" to="/persons" />
          <q-breadcrumbs-el :label="person ? fullName(person) : id" />
        </q-breadcrumbs>
        <q-space />
        <q-btn v-if="origin" flat dense no-caps icon="arrow_back" :label="t('back')" :to="origin.path" />
      </q-toolbar>

      <q-spinner-dots v-if="loading" color="primary" size="2em" class="q-ma-md" />
      <drawer-layout v-else-if="person" class="col">
        <template #drawer>
          <q-list padding role="none">
            <q-item
              v-for="item in MENU"
              :key="item.name"
              clickable
              v-ripple
              :active="tab === item.name"
              :to="tabRoute(item.name)"
            >
              <q-item-section avatar>
                <q-icon :name="item.icon" />
              </q-item-section>
              <q-item-section>{{ t(item.label) }}</q-item-section>
            </q-item>
          </q-list>
        </template>

        <div class="row items-center q-mb-md">
          <div class="col text-caption text-grey-7">
            {{ t('last_modified') }}: {{ getDateLabel(person.timestamps?.lastUpdate) }}
          </div>
          <div class="col-auto q-gutter-sm">
            <q-btn color="primary" icon="edit" :label="t('edit')" size="sm" :disable="busy" @click="showEdit = true" />
            <q-btn
              outline
              color="negative"
              icon="delete"
              :label="t('delete')"
              size="sm"
              :disable="busy"
              @click="showDelete = true"
            />
          </div>
        </div>
        <q-tab-panels v-model="tab">
          <q-tab-panel name="view" class="q-pa-none">
            <person-form :model-value="toPersonModel(person)" readonly />
            <div class="text-h6 q-mt-lg">{{ t('persons.memberships') }}</div>
            <q-banner v-if="!hasMemberships" dense class="bg-orange-1 q-mt-sm">
              {{ t('persons.no_memberships', { name: fullName(person) }) }}
            </q-banner>
            <person-memberships-panel
              v-for="kind in MEMBERSHIP_KINDS"
              :key="kind"
              :person="person"
              :kind="kind"
              :busy="busy"
              class="q-mt-md"
              @change="onChange"
            />
          </q-tab-panel>
          <q-tab-panel name="history" class="q-pa-none">
            <person-history-panel :id="id" />
          </q-tab-panel>
        </q-tab-panels>
      </drawer-layout>
      <div v-else class="q-pa-md">
        {{ t('persons.not_found') }}
      </div>
    </q-page>

    <person-dialog v-model="showEdit" :person="person" @saved="person = $event" />
    <confirm-dialog
      v-model="showDelete"
      :title="t('persons.delete_title')"
      :text="t('persons.delete_text', { name: fullName(person) })"
      @confirm="onDelete"
    />
  </div>
</template>

<script setup lang="ts">
import type { PersonDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import DrawerLayout from 'src/components/DrawerLayout.vue';
import PersonDialog from 'src/components/persons/PersonDialog.vue';
import PersonForm from 'src/components/persons/PersonForm.vue';
import PersonHistoryPanel from 'src/components/persons/PersonHistoryPanel.vue';
import PersonMembershipsPanel from 'src/components/persons/PersonMembershipsPanel.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { getDateLabel } from 'src/utils/dates';
import { notifyError } from 'src/utils/notify';
import { fullName, MEMBERSHIP_KINDS, membershipChanges, personOrigin, toPersonModel } from 'src/utils/persons';
import { usePersonsStore } from 'src/stores/persons';

const MENU = [
  { name: 'view', icon: 'visibility', label: 'view' },
  { name: 'history', icon: 'history', label: 'history.title' },
];

const personsStore = usePersonsStore();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const id = computed(() => route.params.id as string);
const tab = computed(() => {
  const name = route.params.tab as string | undefined;
  return MENU.some((item) => item.name === name) ? (name as string) : 'view';
});

const person = ref<PersonDto>();
const loading = ref(true);
const busy = ref(false);
const showDelete = ref(false);
const showEdit = ref(false);

const hasMemberships = computed(
  () => (person.value?.studyMemberships?.length ?? 0) + (person.value?.networkMemberships?.length ?? 0) > 0,
);

/** the entity page the person page was opened from (members of a study or network), to go back to it */
const origin = computed(() => personOrigin(route.query.from));
const originTarget = computed(() => (origin.value ? documentTarget(origin.value.type, origin.value.id) : undefined));

function tabRoute(name: string) {
  const path = name === 'view' ? `/persons/${id.value}` : `/persons/${id.value}/${name}`;
  return { path, query: origin.value ? { from: origin.value.path } : {} };
}

async function initialize() {
  loading.value = true;
  try {
    person.value = await personsStore.get(id.value);
  } catch (error) {
    person.value = undefined;
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

/** the memberships changed: each one is saved on its own, the other memberships of the person are not touched */
async function onChange(changed: PersonDto) {
  if (!person.value) return;
  const { added, removed } = membershipChanges(person.value, changed);
  busy.value = true;
  try {
    // added first: a failed add leaves the old role in place, not a person without the link
    for (const m of added) await personsStore.addRole(id.value, m.parent, m.parentId, m.role);
    for (const m of removed) await personsStore.removeRole(id.value, m.parent, m.parentId, m.role);
  } catch (error) {
    notifyError(error);
  } finally {
    // what was saved, also when a change failed
    try {
      person.value = await personsStore.get(id.value);
    } catch (error) {
      notifyError(error);
    }
    busy.value = false;
  }
}

async function onDelete() {
  busy.value = true;
  try {
    await personsStore.remove(id.value);
    if (origin.value) {
      // the entity members are read from the database, the deleted person is gone
      await router.replace(origin.value.path);
    } else {
      // the search index may still have the person for a moment
      await router.replace({ path: '/persons', query: { exclude: id.value } });
    }
  } catch (error) {
    notifyError(error);
  } finally {
    busy.value = false;
  }
}

watch(id, initialize, { immediate: true });
</script>
