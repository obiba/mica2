<template>
  <div>
    <div class="row">
      <div class="col-12 col-md-4">
        <q-img v-if="network.logo" :src="logoUrl" class="q-mb-md" style="max-width: 200px" />
      </div>
      <div class="col-12 col-md-8">
        <localized-input :model-value="network.acronym" :label="t('acronym')" readonly />
        <localized-input :model-value="network.name" :label="t('name')" readonly />
      </div>
    </div>
    <localized-input :model-value="network.description" :label="t('description')" :rows="10" readonly />
    <pre>{{ network }}</pre>
  </div>
</template>

<script setup lang="ts">
import type { NetworkDto } from 'src/models/Mica';
import { toServerUrl } from 'src/boot/api';
import LocalizedInput from 'src/components/commons/LocalizedInput.vue';

interface Props {
  network: NetworkDto;
}

const props = defineProps<Props>();
const { t } = useI18n();

const logoUrl = computed(() =>
  props.network.logo ? toServerUrl(`/draft/network/${props.network.id}/file/${props.network.logo.id}/_download`) : '',
);
</script>
