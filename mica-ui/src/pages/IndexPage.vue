<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="row items-start">
        <div class="col">
          <div class="text-h5 q-mb-sm">{{ t('dashboard.title') }}</div>
          <div class="text-grey-8 q-mb-lg">{{ t('dashboard.info') }}</div>
        </div>
        <div v-if="!failed" class="col-auto text-right">
          <q-btn
            color="primary"
            icon="refresh"
            :label="t('config.statistics.refresh')"
            size="sm"
            :loading="loading"
            @click="load"
          />
          <div v-if="refreshedAt" class="text-hint q-mt-xs">
            {{ t('config.statistics.refreshed_at', { time: refreshedAt.toLocaleTimeString() }) }}
          </div>
        </div>
      </div>

      <q-banner v-if="failed" rounded class="bg-negative text-white q-mb-md">
        <template v-slot:avatar>
          <q-icon name="error" />
        </template>
        {{ t('config.statistics.load_failed') }}
        <template v-slot:action>
          <q-btn flat :label="t('config.statistics.refresh')" :loading="loading" @click="load" />
        </template>
      </q-banner>

      <div v-if="loading && !metrics" class="row q-col-gutter-md">
        <div v-for="type in DASHBOARD_TYPES" :key="type" class="col-12 col-md-6 col-lg-4">
          <q-card flat bordered>
            <q-card-section>
              <q-skeleton type="text" width="40%" />
              <q-skeleton type="rect" height="48px" width="30%" class="q-mt-md" />
              <q-skeleton type="rect" height="10px" class="q-mt-md" />
              <q-skeleton type="text" class="q-mt-md" />
            </q-card-section>
          </q-card>
        </div>
      </div>
      <div v-else-if="!failed && metrics && cards.length === 0" class="text-hint q-mb-lg">
        {{ t('config.statistics.none') }}
      </div>
      <div v-else class="row q-col-gutter-md">
        <div v-for="typeMetrics in cards" :key="typeMetrics.type" class="col-12 col-md-6 col-lg-4">
          <metrics-type-card :metrics="typeMetrics" hide-index-health />
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import MetricsTypeCard from 'src/components/settings/statistics/MetricsTypeCard.vue';
import { useContentMetrics, type MetricsType, type TypeMetrics } from 'src/composables/useContentMetrics';

const DASHBOARD_TYPES: MetricsType[] = [
  'Network',
  'Study',
  'HarmonizationStudy',
  'StudyDataset',
  'HarmonizationDataset',
];

const { t } = useI18n();
const { loading, failed, metrics, refreshedAt, load } = useContentMetrics();

/** the metrics of the dashboard types, in dashboard order; the types disabled in the config are not served */
const cards = computed(() =>
  DASHBOARD_TYPES.map((type) => metrics.value?.types.find((m) => m.type === type)).filter(
    (m): m is TypeMetrics => m !== undefined,
  ),
);

onMounted(load);
</script>
