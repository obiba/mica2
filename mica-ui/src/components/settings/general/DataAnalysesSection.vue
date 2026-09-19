<template>
  <config-section :title="t('config.data_analyses')" :help="t('config.data_analyses_help')" :items="items">
    <template #form="{ form }">
      <config-toggle v-model="form.isContingencyEnabled" name="contingency_enabled" />
      <config-toggle v-model="form.isSetsAnalysisEnabled" name="sets_analysis_enabled" />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem } from 'src/components/settings/general/fields';

const { t } = useI18n();

const items = [
  flagItem('isContingencyEnabled', 'contingency_enabled'),
  flagItem('isSetsAnalysisEnabled', 'sets_analysis_enabled'),
];
</script>
