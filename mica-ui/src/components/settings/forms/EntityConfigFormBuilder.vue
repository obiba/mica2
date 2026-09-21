<template>
  <config-form-builder ref="builder" :state="state" :info="info" />
</template>

<script setup lang="ts">
import ConfigFormBuilder from 'src/components/settings/forms/ConfigFormBuilder.vue';
import { useEntityConfigForm, type EntityConfigTarget } from 'src/composables/useEntityConfigForm';

interface Props {
  /** the form configuration edited, owned by the builder for as long as it is mounted (key the builder by its name) */
  target: EntityConfigTarget;
  /** what the form is about, may contain HTML from the app bundles */
  info: string;
}

const props = defineProps<Props>();

const state = useEntityConfigForm(props.target);
const builder = ref<InstanceType<typeof ConfigFormBuilder>>();

function confirmLeave(): Promise<boolean> {
  return builder.value?.confirmLeave() ?? Promise.resolve(true);
}

defineExpose({ confirmLeave });
</script>
