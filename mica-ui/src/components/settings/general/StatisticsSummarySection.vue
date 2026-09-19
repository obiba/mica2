<template>
  <config-section :title="t('config.statistics_summary')" :items="items">
    <template #form="{ form }">
      <config-toggle v-model="form.isVariablesCountEnabled" name="variables_count_enabled" />
      <config-toggle
        v-model="form.isProjectsCountEnabled"
        name="projects_count_enabled"
        :disable="!form.isProjectEnabled"
      />
      <config-toggle
        v-model="form.isDataAccessRequestsCountEnabled"
        name="data_access_requests_count_enabled"
        :disable="!form.isDataAccessEnabled"
      />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem } from 'src/components/settings/general/fields';

const { t } = useI18n();

const items = [
  flagItem('isVariablesCountEnabled', 'variables_count_enabled'),
  flagItem('isProjectsCountEnabled', 'projects_count_enabled'),
  flagItem('isDataAccessRequestsCountEnabled', 'data_access_requests_count_enabled'),
];
</script>
