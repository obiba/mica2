<template>
  <div class="row q-col-gutter-md">
    <div v-if="network.logo" class="col-12 col-md-3">
      <q-img :src="downloadUrl(network.logo, target.path)" style="max-width: 200px" fit="contain" />
    </div>
    <div class="col">
      <entity-json-form :model-value="model" :form-path="target.formPath" readonly />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NetworkDto } from 'src/models/Mica';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentModel } from 'src/composables/useDocumentModel';
import { useTempFiles } from 'src/composables/useTempFiles';

interface Props {
  network: NetworkDto;
}

const props = defineProps<Props>();
const { toModel } = useDocumentModel('network');
const { downloadUrl } = useTempFiles();

const target = computed(() => documentTarget('network', props.network.id ?? ''));
const model = computed(() => toModel(props.network));
</script>
