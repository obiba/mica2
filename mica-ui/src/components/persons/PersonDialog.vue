<template>
  <q-dialog v-model="show" persistent @before-show="onShow">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ person ? fullName(person) : t('persons.new') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll">
        <person-form ref="form" v-model="model" />
        <div v-if="invalid" class="text-negative q-mt-sm">{{ t('missing_required_fields') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" :disable="saving" @click="onCancel" />
        <q-btn flat :label="t('save')" color="primary" :loading="saving" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { useQuasar } from 'quasar';
import type { PersonDto } from 'src/models/Mica';
import PersonForm from 'src/components/persons/PersonForm.vue';
import type { FormModel } from 'src/composables/useDocumentModel';
import { usePersonsStore } from 'src/stores/persons';
import { notifyError } from 'src/utils/notify';
import { fromPersonModel, fullName, toPersonModel } from 'src/utils/persons';

interface Props {
  /** the person to edit, a new one when undefined */
  person?: PersonDto | undefined;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ saved: [person: PersonDto] }>();
const personsStore = usePersonsStore();
const $q = useQuasar();
const { t } = useI18n();

const form = ref<InstanceType<typeof PersonForm>>();
const saving = ref(false);
const invalid = ref(false);
const model = ref<FormModel>({});
/** snapshot of what was opened, for the dirty check */
let snapshot = '';

function original(): PersonDto {
  return props.person ?? { lastName: '', studyMemberships: [], networkMemberships: [] };
}

function onShow() {
  invalid.value = false;
  model.value = toPersonModel(original());
  snapshot = JSON.stringify(model.value);
}

function confirm(title: string, message: string) {
  return new Promise<boolean>((resolve) => {
    $q.dialog({ title, message, cancel: true, persistent: true })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false));
  });
}

async function onCancel() {
  if (
    JSON.stringify(model.value) !== snapshot &&
    !(await confirm(t('document.unsaved_title'), t('document.unsaved_text')))
  )
    return;
  show.value = false;
}

async function onSave() {
  invalid.value = !form.value?.validate();
  if (invalid.value) return;
  const before = original();
  const dto = fromPersonModel(before, model.value);
  saving.value = true;
  try {
    // as the legacy admin app, when the identity changes: same name and same email is a duplicate,
    // same name only may be one
    const changed =
      !props.person || (['firstName', 'lastName', 'email'] as const).some((field) => dto[field] !== before[field]);
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
    const result = props.person ? await personsStore.update(dto) : await personsStore.create(dto);
    show.value = false;
    emit('saved', result);
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}
</script>
