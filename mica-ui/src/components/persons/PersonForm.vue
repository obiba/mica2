<template>
  <q-json-form
    :model-value="modelValue"
    :schema="schema"
    :uischema="uischema"
    :readonly="readonly"
    :languages="systemStore.languages"
    :validation-mode="validationMode"
    @update:model-value="emit('update:modelValue', $event)"
    @update:errors="errors = $event"
  />
</template>

<script setup lang="ts">
import type { ErrorObject } from 'ajv';
import type { ValidationMode } from '@jsonforms/core';
import { QJsonForm } from '@obiba/quasar-ui-json-form';
import type { FormModel } from 'src/composables/useDocumentModel';

interface Props {
  modelValue: FormModel;
  readonly?: boolean;
}

defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [model: FormModel] }>();
const { t } = useI18n();
const systemStore = useSystemStore();

const field = (key: string) => t(`persons.field.${key}`);
const text = (key: string) => ({ title: field(key), type: 'string' });
const localized = (key: string) => ({ title: field(key), type: 'object', format: 'localizedString' });
const control = (path: string) => ({ type: 'Control', scope: `#/properties/${path.split('.').join('/properties/')}`, options: { dense: true } });
const column = (title: string, paths: string[]) => ({
  type: 'VerticalLayout',
  options: { class: 'col-12 col-md-6' },
  elements: [{ type: 'Label', text: `<h6>${t(`persons.${title}`)}</h6>` }, ...paths.map(control)],
});

const schema = computed(() => ({
  type: 'object',
  properties: {
    title: text('title'),
    firstName: text('first_name'),
    lastName: text('last_name'),
    academicLevel: text('academic_level'),
    email: { ...text('email'), pattern: '^\\S+@\\S+$' },
    phone: text('phone'),
    institution: {
      type: 'object',
      properties: {
        name: localized('institution_name'),
        department: localized('department'),
        address: {
          type: 'object',
          properties: {
            street: localized('street'),
            city: localized('city'),
            zip: text('zip'),
            state: text('state'),
            country: { ...text('country'), format: 'obibaCountriesUiSelect' },
          },
        },
      },
    },
  },
  required: ['lastName'],
}));

const uischema = computed(() => ({
  type: 'VerticalLayout',
  options: { class: 'row q-col-gutter-md' },
  elements: [
    column('identification', ['title', 'firstName', 'lastName', 'academicLevel', 'email', 'phone']),
    column('institution', [
      'institution.name',
      'institution.department',
      'institution.address.street',
      'institution.address.city',
      'institution.address.zip',
      'institution.address.state',
      'institution.address.country',
    ]),
  ],
}));

// errors are shown after the first explicit validation only, as in the documents forms
const validationMode = ref<ValidationMode>('ValidateAndHide');
const errors = ref<ErrorObject[]>([]);

/** shows the errors and tells whether the form is valid */
function validate(): boolean {
  validationMode.value = 'ValidateAndShow';
  return errors.value.length === 0;
}

defineExpose({ validate });
</script>
