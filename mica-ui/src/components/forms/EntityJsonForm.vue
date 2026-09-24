<template>
  <div>
    <q-spinner-dots v-if="loading && !converted" color="primary" size="2em" />
    <q-json-form
      v-if="converted"
      :model-value="modelValue"
      :schema="converted.schema"
      :uischema="converted.uischema"
      :readonly="readonly"
      :languages="languages"
      :validation-mode="validationMode"
      :additional-errors="additionalErrors ?? []"
      @update:model-value="emit('update:modelValue', $event)"
      @update:errors="onErrors"
    />
  </div>
</template>

<script setup lang="ts">
import type { ErrorObject } from 'ajv';
import type { ValidationMode } from '@jsonforms/core';
import { QJsonForm } from '@obiba/quasar-ui-json-form';
import { useEntityForm } from 'src/composables/useEntityForm';
import type { FormModel } from 'src/composables/useDocumentModel';

interface Props {
  modelValue: FormModel;
  /** REST path of the form configuration: `/config/network/form` */
  formPath: string;
  readonly?: boolean;
  /** errors found apart from the schema ones (unique id...), shown on their controls */
  additionalErrors?: ErrorObject[];
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [model: FormModel];
  'update:errors': [errors: ErrorObject[]];
}>();

const systemStore = useSystemStore();
const languages = computed(() => systemStore.languages);

const { loading, schema, uischema } = useEntityForm(() => props.formPath, { readonly: () => props.readonly === true });
const converted = computed(() =>
  schema.value && uischema.value ? { schema: schema.value, uischema: uischema.value } : undefined,
);

// errors are shown after the first explicit validation only (as the legacy `schemaFormValidate`
// broadcast did): a new document must not open with every required field in red
const validationMode = ref<ValidationMode>('ValidateAndHide');
const errors = ref<ErrorObject[]>([]);

function onErrors(value: ErrorObject[]) {
  errors.value = value;
  emit('update:errors', value);
}

/** shows the errors and tells whether the form is valid (schema and renderer-level checks) */
function validate(): boolean {
  validationMode.value = 'ValidateAndShow';
  return errors.value.length === 0;
}

defineExpose({ validate, errors });
</script>
