<template>
  <div class="q-mb-md">
    <q-toggle v-model="model" dense :label="t(`config.${name}`)" :disable="disable" />
    <div v-if="!noHelp" class="text-hint">{{ t(`config.${name}_help`) }}</div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** the message key under `config`: the label, and `<name>_help` the hint */
  name: string;
  disable?: boolean;
  noHelp?: boolean;
}

defineProps<Props>();
const model = defineModel<boolean | undefined>({ default: false });
const { t } = useI18n();
</script>
