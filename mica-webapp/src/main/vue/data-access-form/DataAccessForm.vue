<template>
  <QJsonForm
    v-model="model"
    v-model:errors="errors"
    :schema="converted.schema"
    :uischema="converted.uischema"
    :readonly="readOnly"
    :languages="languages"
    :validation-mode="validationMode"
    :config="config"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import type { ErrorObject } from 'ajv';
import type { ValidationMode } from '@jsonforms/core';
import { QJsonForm, toJsonForms } from '@obiba/quasar-ui-json-form';
import type { AsfDiagnostic, FileUploadHooks } from '@obiba/quasar-ui-json-form';

const props = defineProps<{
  schema: Record<string, any>;
  /** a JSON Forms UI schema (object), or an angular-schema-form definition (array) converted on the fly */
  definition: unknown;
  modelValue: Record<string, any>;
  readOnly: boolean;
  /** page language: the localized strings are edited in that language only, as the ASF page did */
  lang: string;
  fileUpload: FileUploadHooks;
}>();

const { t } = useI18n();

const model = ref<Record<string, any>>(props.modelValue);
// `{ code: label }`, the label being the Mica `language.<code>` translation once loaded
const languages = computed(() => ({ [props.lang]: t(`language.${props.lang}`) }));
const errors = ref<ErrorObject[]>([]);

// Errors are shown after the first explicit validation only (validate / submit buttons), like
// the ASF `schemaFormValidate` broadcast: an empty request must not open with every required
// field in red.
const validationMode = ref<ValidationMode>('ValidateAndHide');

// a UI schema is rendered as it is; the ASF definition of an older form revision is converted
const converted = computed(() => {
  const result = toJsonForms(props.schema, props.definition, {
    readonly: props.readOnly,
    languages: languages.value,
    logger: (diagnostic: AsfDiagnostic) => {
      const log = diagnostic.level === 'warn' ? console.warn : console.info;
      log(`[data-access-form] ${diagnostic.message}`, diagnostic.key ?? '', diagnostic.element ?? '');
    },
  });
  return { schema: result.schema, uischema: result.uischema };
});

const config = computed(() => ({ fileUpload: props.fileUpload }));

/** shows the errors and tells whether the form is valid (schema + renderer-level checks) */
function validate(): boolean {
  validationMode.value = 'ValidateAndShow';
  return errors.value.length === 0;
}

function getModel(): Record<string, any> {
  return model.value;
}

function getErrors(): ErrorObject[] {
  return errors.value;
}

defineExpose({ validate, getModel, getErrors });
</script>
