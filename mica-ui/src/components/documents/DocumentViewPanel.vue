<template>
  <div class="row q-col-gutter-md">
    <div v-if="logo" class="col-12 col-md-3">
      <q-img :src="downloadUrl(logo, target.path)" style="max-width: 200px" fit="contain" />
    </div>
    <div class="col">
      <entity-json-form :model-value="model" :form-path="target.formPath" readonly />
    </div>
  </div>
</template>

<script setup lang="ts">
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentModel } from 'src/composables/useDocumentModel';
import { useTempFiles } from 'src/composables/useTempFiles';
import type { DocumentDto } from 'src/stores/documents';

interface Props {
  target: DocumentTarget;
  document: DocumentDto;
}

const props = defineProps<Props>();
const { toModel } = useDocumentModel(() => props.target.type);
const { downloadUrl } = useTempFiles();

const logo = computed(() => (props.target.withLogo && 'logo' in props.document ? props.document.logo : undefined));
const model = computed(() => toModel(props.document));
</script>
