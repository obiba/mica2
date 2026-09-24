<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('network_members.add_title', { role }) }}</div>
        <div class="text-hint">{{ t('network_members.add_help') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-select
          v-model="person"
          :options="options"
          :option-label="optionLabel"
          option-value="id"
          :label="t('network_members.person')"
          use-input
          input-debounce="300"
          dense
          @filter="onFilter"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">{{ t('persons.none') }}</q-item-section>
            </q-item>
          </template>
        </q-select>
      </q-card-section>
      <q-separator />
      <q-card-actions class="bg-grey-3">
        <q-btn flat icon="add" :label="t('persons.new')" color="primary" @click="showNew = true" />
        <q-space />
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn flat :label="t('add')" color="primary" :disable="!person" v-close-popup @click="onAdd" />
      </q-card-actions>
    </q-card>
    <person-dialog v-model="showNew" @saved="onCreated" />
  </q-dialog>
</template>

<script setup lang="ts">
import type { PersonDto } from 'src/models/Mica';
import PersonDialog from 'src/components/persons/PersonDialog.vue';
import { usePersonsStore } from 'src/stores/persons';
import { notifyError } from 'src/utils/notify';
import { fullName, localized, searchQuery } from 'src/utils/persons';

interface Props {
  role: string;
  /** the ids of the persons already members with the role */
  exclude: string[];
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ add: [person: PersonDto] }>();
const { t, locale } = useI18n();
const personsStore = usePersonsStore();

const person = ref<PersonDto>();
const options = ref<PersonDto[]>([]);
const showNew = ref(false);

function optionLabel(option: PersonDto) {
  const institution = localized(option.institution?.name, locale.value);
  return [fullName(option), option.email, institution].filter((part) => part).join(' - ');
}

function onShow() {
  person.value = undefined;
  options.value = [];
}

function onFilter(text: string, update: (callback: () => void) => void, abort: () => void) {
  personsStore
    .search({ query: searchQuery(text), from: 0, limit: 20, sort: 'lastName', order: 'asc' })
    .then((result) => {
      update(() => {
        options.value = result.persons.filter((option) => !props.exclude.includes(option.id ?? ''));
      });
    })
    .catch((error) => {
      abort();
      notifyError(error);
    });
}

function onAdd() {
  if (person.value) emit('add', person.value);
}

function onCreated(created: PersonDto) {
  show.value = false;
  emit('add', created);
}
</script>
