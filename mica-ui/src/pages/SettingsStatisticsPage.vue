<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.statistics')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="row items-start">
        <div class="col">
          <div class="text-h5 q-mb-sm">{{ t('config.statistics.title') }}</div>
          <p class="text-grey-8">{{ t('config.statistics.info') }}</p>
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

      <q-banner v-if="metrics && metrics.notIndexed > 0" rounded class="bg-warning text-dark q-mb-md">
        <template v-slot:avatar>
          <q-icon name="warning" />
        </template>
        {{ t('config.statistics.require_indexing_warning', metrics.notIndexed) }}
      </q-banner>

      <div v-if="loading && !metrics" class="row q-col-gutter-md">
        <div v-for="i in 6" :key="i" class="col-12 col-md-6 col-lg-4">
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
      <p v-else-if="!failed && (!metrics || metrics.types.length === 0)" class="text-hint">
        {{ t('config.statistics.none') }}
      </p>
      <div v-else-if="metrics" class="row q-col-gutter-md">
        <div v-for="typeMetrics in metrics.types" :key="typeMetrics.type" class="col-12 col-md-6 col-lg-4">
          <variables-metrics-card v-if="typeMetrics.type === 'DatasetVariable'" :metrics="typeMetrics" />
          <data-access-metrics-card v-else-if="typeMetrics.type === 'DataAccessRequest'" :metrics="typeMetrics" />
          <metrics-type-card v-else :metrics="typeMetrics" @index="onIndex" />
        </div>
      </div>

      <index-health-dialog
        v-model="showIndexDialog"
        :type="indexType"
        :load="loadIndexHealth"
        :index="index"
        @indexed="onIndexed"
      />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import DataAccessMetricsCard from 'src/components/settings/statistics/DataAccessMetricsCard.vue';
import IndexHealthDialog from 'src/components/settings/statistics/IndexHealthDialog.vue';
import MetricsTypeCard from 'src/components/settings/statistics/MetricsTypeCard.vue';
import VariablesMetricsCard from 'src/components/settings/statistics/VariablesMetricsCard.vue';
import { useContentMetrics, type MetricsType } from 'src/composables/useContentMetrics';
import { notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const { loading, failed, metrics, refreshedAt, load, loadIndexHealth, index } = useContentMetrics();

const showIndexDialog = ref(false);
const indexType = ref<MetricsType>();

function onIndex(type: MetricsType) {
  indexType.value = type;
  showIndexDialog.value = true;
}

function onIndexed(count: number) {
  notifySuccess(t('config.statistics.index_dialog.started', { count }));
}

onMounted(load);
</script>
