<template>
  <div class="row q-col-gutter-md">
    <div class="col-12 col-md-6">
      <server-identification-section />
      <content-edition-section />
      <content-access-section />
      <sets-section />
      <compare-export-section />
    </div>
    <div class="col-12 col-md-6">
      <data-source-section />
      <enabled-sections-section />
      <data-analyses-section />
      <statistics-summary-section />
    </div>
  </div>
</template>

<script setup lang="ts">
import ServerIdentificationSection from 'src/components/settings/general/ServerIdentificationSection.vue';
import DataSourceSection from 'src/components/settings/general/DataSourceSection.vue';
import ContentEditionSection from 'src/components/settings/general/ContentEditionSection.vue';
import ContentAccessSection from 'src/components/settings/general/ContentAccessSection.vue';
import EnabledSectionsSection from 'src/components/settings/general/EnabledSectionsSection.vue';
import SetsSection from 'src/components/settings/general/SetsSection.vue';
import CompareExportSection from 'src/components/settings/general/CompareExportSection.vue';
import DataAnalysesSection from 'src/components/settings/general/DataAnalysesSection.vue';
import StatisticsSummarySection from 'src/components/settings/general/StatisticsSummarySection.vue';
</script>
