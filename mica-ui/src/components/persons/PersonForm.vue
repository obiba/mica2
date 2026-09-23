<template>
  <q-json-form
    :model-value="modelValue"
    :schema="converted.schema"
    :uischema="converted.uischema"
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
import { QJsonForm, toJsonForms } from '@obiba/quasar-ui-json-form';
import type { FormModel } from 'src/composables/useDocumentModel';

interface Props {
  modelValue: FormModel;
  readonly?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [model: FormModel] }>();
const { t } = useI18n();
const systemStore = useSystemStore();

// the contact form of the legacy admin app (`app/contact/contact-schemaform.js`)
const LOCALIZED = { type: 'object', format: 'localizedString' };
const SCHEMA = {
  type: 'object',
  properties: {
    title: { title: 't(title)', type: 'string' },
    firstName: { title: 't(first_name)', type: 'string' },
    lastName: { title: 't(last_name)', type: 'string' },
    academicLevel: { title: 't(academic_level)', type: 'string' },
    email: { title: 't(email)', type: 'string', pattern: '^\\S+@\\S+$' },
    phone: { title: 't(phone)', type: 'string' },
    institution: {
      type: 'object',
      properties: {
        name: { title: 't(institution_name)', ...LOCALIZED },
        department: { title: 't(department)', ...LOCALIZED },
        address: {
          type: 'object',
          properties: {
            street: { title: 't(street)', ...LOCALIZED },
            city: { title: 't(city)', ...LOCALIZED },
            zip: { title: 't(zip)', type: 'string' },
            state: { title: 't(state)', type: 'string' },
            country: { title: 't(country)', type: 'string', format: 'obibaCountriesUiSelect' },
          },
        },
      },
    },
  },
  required: ['lastName'],
};
const DEFINITION = [
  {
    type: 'section',
    htmlClass: 'row',
    items: [
      {
        type: 'section',
        htmlClass: 'col-xs-12 col-md-6',
        items: [
          { type: 'help', helpvalue: '<h6>t(identification)</h6>' },
          'title',
          'firstName',
          'lastName',
          'academicLevel',
          'email',
          'phone',
        ],
      },
      {
        type: 'section',
        htmlClass: 'col-xs-12 col-md-6',
        items: [
          { type: 'help', helpvalue: '<h6>t(institution)</h6>' },
          { key: 'institution.name', type: 'localizedstring' },
          { key: 'institution.department', type: 'localizedstring' },
          { key: 'institution.address.street', type: 'localizedstring' },
          { key: 'institution.address.city', type: 'localizedstring' },
          'institution.address.zip',
          'institution.address.state',
          { key: 'institution.address.country', type: 'obibaCountriesUiSelect' },
        ],
      },
    ],
  },
];

const converted = computed(() =>
  toJsonForms(SCHEMA, DEFINITION, {
    readonly: props.readonly === true,
    translate: (key: string) =>
      ['identification', 'institution'].includes(key) ? t(`persons.${key}`) : t(`persons.field.${key}`),
  }),
);

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
