<template>
  <div v-if="state.dto" class="q-mt-lg">
    <div class="text-h6">{{ t('config.data_access.properties') }}</div>
    <p class="text-grey-8">{{ info }}</p>
    <div class="row q-col-gutter-md">
      <div v-for="field in FIELDS" :key="field" class="col-12 col-md-4">
        <config-input
          :model-value="state.dto[field]"
          :name="`data_access.${prefix}${field}`"
          @update:model-value="set(field, $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import type { ConfigFormState } from 'src/composables/useConfigForm';

/** the fields of the request extracted from the form model by path */
const FIELDS = ['titleFieldPath', 'summaryFieldPath', 'endDateFieldPath'] as const;
type Field = (typeof FIELDS)[number];

interface Props {
  /** the form whose DTO holds the field paths */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  state: ConfigFormState<any>;
  /** what the properties are about */
  info: string;
  /** the message key prefix of the fields under `config.data_access` (`amendment_` for the amendment) */
  prefix?: string;
}

const props = withDefaults(defineProps<Props>(), { prefix: '' });
const { t } = useI18n();

const state = reactive(props.state);

/** an empty path is not sent (the server keeps none) */
function set(field: Field, value: string | number | undefined) {
  if (!state.dto) return;
  const text = value === undefined || value === null ? '' : String(value).trim();
  (state.dto as Record<Field, string | undefined>)[field] = text === '' ? undefined : text;
  state.dirty = true;
}
</script>
