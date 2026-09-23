<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('persons.title')" to="/persons" />
        <q-breadcrumbs-el v-if="id" :label="fullName(person) || id" :to="`/persons/${id}`" />
        <q-breadcrumbs-el :label="id ? t('edit') : t('persons.new')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-spinner-dots v-if="loading" color="primary" size="2em" />
      <div v-else-if="person">
        <person-form ref="form" v-model="model" />
        <div v-if="invalid" class="text-negative q-mt-sm">{{ t('missing_required_fields') }}</div>
        <document-save-bar :saving="saving" @save="onSave" @cancel="onCancel" />
      </div>
      <div v-else>
        {{ t('persons.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import { onBeforeRouteLeave } from 'vue-router';
import { useQuasar } from 'quasar';
import type { PersonDto } from 'src/models/Mica';
import DocumentSaveBar from 'src/components/documents/DocumentSaveBar.vue';
import PersonForm from 'src/components/persons/PersonForm.vue';
import type { FormModel } from 'src/composables/useDocumentModel';
import { notifyError } from 'src/utils/notify';
import { fromPersonModel, fullName, toPersonModel } from 'src/utils/persons';
import { usePersonsStore } from 'src/stores/persons';

const personsStore = usePersonsStore();
const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { t } = useI18n();

/** undefined for a new person */
const id = computed(() => route.params.id as string | undefined);

const form = ref<InstanceType<typeof PersonForm>>();
const loading = ref(true);
const saving = ref(false);
const invalid = ref(false);
const person = ref<PersonDto>();
const model = ref<FormModel>({});
/** snapshot of what was loaded, for the dirty check */
let snapshot = '';
let saved = false;

async function initialize() {
  loading.value = true;
  try {
    const loaded: PersonDto = id.value
      ? await personsStore.get(id.value)
      : { lastName: '', studyMemberships: [], networkMemberships: [] };
    person.value = loaded;
    model.value = toPersonModel(loaded);
    snapshot = JSON.stringify(model.value);
  } catch (error) {
    person.value = undefined;
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

function confirm(title: string, message: string) {
  return new Promise<boolean>((resolve) => {
    $q.dialog({ title, message, cancel: true, persistent: true })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false));
  });
}

async function onSave() {
  if (!person.value) return;
  invalid.value = !form.value?.validate();
  if (invalid.value) return;
  const dto = fromPersonModel(person.value, model.value);
  saving.value = true;
  try {
    // as the legacy admin app, when the identity changes: same name and same email is a duplicate,
    // same name only may be one
    const original = person.value;
    const changed =
      !id.value || (['firstName', 'lastName', 'email'] as const).some((field) => dto[field] !== original[field]);
    const duplicates = changed ? await personsStore.findDuplicates(dto) : {};
    if (duplicates.sameEmail) {
      $q.notify({ type: 'negative', message: t('persons.duplicate_email', { email: duplicates.sameEmail }) });
      return;
    }
    if (
      duplicates.sameName &&
      !(await confirm(
        t('persons.duplicate_name_title'),
        t('persons.duplicate_name_text', { name: duplicates.sameName }),
      ))
    )
      return;
    const result = id.value ? await personsStore.update(dto) : await personsStore.create(dto);
    saved = true;
    await router.replace(`/persons/${result.id}`);
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}

function onCancel() {
  router.replace(id.value ? `/persons/${id.value}` : '/persons');
}

onBeforeRouteLeave(() => {
  if (saved || JSON.stringify(model.value) === snapshot) return true;
  return confirm(t('document.unsaved_title'), t('document.unsaved_text'));
});

onMounted(initialize);
</script>
