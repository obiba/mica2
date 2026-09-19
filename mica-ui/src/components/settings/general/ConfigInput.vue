<template>
  <q-input
    :model-value="modelValue"
    dense
    outlined
    :type="type"
    :label="t(`config.${name}`) + (required ? ' *' : '')"
    :hint="noHelp ? undefined : t(`config.${name}_help`)"
    :rules="rules"
    :disable="disable"
    :min="min"
    :step="type === 'number' ? 1 : undefined"
    lazy-rules
    class="q-mb-md"
    @update:model-value="onUpdate"
  />
</template>

<script setup lang="ts">
import type { ValidationRule } from 'quasar';

interface Props {
  modelValue: string | number | undefined;
  /** the message key under `config`: the label, and `<name>_help` the hint */
  name: string;
  type?: 'text' | 'number' | 'url';
  required?: boolean;
  min?: number;
  disable?: boolean;
  noHelp?: boolean;
}

const props = withDefaults(defineProps<Props>(), { type: 'text' });
const emit = defineEmits<{ 'update:modelValue': [value: string | number | undefined] }>();
const { t } = useI18n();

function isBlank(value: unknown) {
  return value === undefined || value === null || String(value).trim() === '';
}

const rules = computed<ValidationRule[]>(() => {
  const list: ValidationRule[] = [];
  if (props.required) list.push((value) => !isBlank(value) || t('required'));
  if (props.type === 'number') {
    list.push(
      (value) =>
        isBlank(value) ||
        (!Number.isNaN(Number(value)) && (props.min === undefined || Number(value) >= props.min)) ||
        t('number_invalid'),
    );
  }
  return list;
});

/** numbers are emitted as numbers, an emptied field as undefined */
function onUpdate(value: string | number | null) {
  if (isBlank(value) || value === null) emit('update:modelValue', undefined);
  else emit('update:modelValue', props.type === 'number' ? Number(value) : value);
}
</script>
