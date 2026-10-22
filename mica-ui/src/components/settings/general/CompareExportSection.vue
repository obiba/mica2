<template>
  <config-section :title="t('config.documents_compare_export')" :items="items">
    <template #form="{ form }">
      <config-toggle v-model="form.isStudiesCompareEnabled" name="studies_compare_enabled" />
      <config-toggle v-model="form.isNetworksCompareEnabled" name="networks_compare_enabled" />
      <config-input v-model="form.maxItemsPerCompare" name="max_items_per_compare" type="number" :min="0" required />
      <config-toggle v-model="form.isStudiesExportEnabled" name="studies_export_enabled" />
      <config-toggle v-model="form.isNetworksExportEnabled" name="networks_export_enabled" />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem, valueItem } from 'src/components/settings/general/fields';

const { t } = useI18n();

const items = [
  flagItem('isStudiesCompareEnabled', 'studies_compare_enabled'),
  flagItem('isNetworksCompareEnabled', 'networks_compare_enabled'),
  valueItem('maxItemsPerCompare', 'max_items_per_compare'),
  flagItem('isStudiesExportEnabled', 'studies_export_enabled'),
  flagItem('isNetworksExportEnabled', 'networks_export_enabled'),
];
</script>
