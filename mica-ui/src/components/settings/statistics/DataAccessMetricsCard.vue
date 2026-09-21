<template>
  <q-card flat bordered class="full-height">
    <q-card-section class="q-pb-none">
      <div class="row items-center no-wrap">
        <q-icon name="fact_check" size="sm" color="grey-7" class="q-mr-sm" />
        <div class="text-subtitle1 text-weight-medium ellipsis">
          <a :href="portalDataAccessesUrl()" class="title-link">{{ t('config.statistics.types.DataAccessRequest') }}</a>
        </div>
      </div>
    </q-card-section>
    <q-card-section>
      <div class="row items-baseline no-wrap">
        <span class="hero-number">{{ formatNumber(total) }}</span>
        <div class="q-ml-sm text-grey-7">
          <div class="text-caption text-uppercase">{{ t('config.statistics.total') }}</div>
        </div>
      </div>
      <div class="q-mt-md">
        <div v-for="row in rows" :key="row.status" class="row items-center no-wrap q-mb-xs status-row">
          <div class="status-label text-caption ellipsis">{{ t(`data_access_request.status.${row.status}`) }}</div>
          <div class="col q-px-sm">
            <div class="status-bar" :style="{ width: `${row.share}%` }" :class="{ 'status-bar--empty': row.value === 0 }"></div>
          </div>
          <div class="status-value text-caption text-weight-medium text-right">{{ formatNumber(row.value) }}</div>
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { DAR_STATUSES, portalDataAccessesUrl, type TypeMetrics } from 'src/composables/useContentMetrics';
import { formatNumber } from 'src/utils/numbers';

/** the data access requests by status, as horizontal bars */
const props = defineProps<{
  metrics: TypeMetrics;
}>();

const { t } = useI18n();

const rows = computed(() => {
  const values = DAR_STATUSES.map((status) => ({ status, value: props.metrics.counts[status] ?? 0 }));
  const max = Math.max(1, ...values.map((row) => row.value));
  return values.map((row) => ({ ...row, share: (100 * row.value) / max }));
});
const total = computed(() => rows.value.reduce((sum, row) => sum + row.value, 0));
</script>

<style scoped lang="scss">
.hero-number {
  font-size: 2.5rem;
  line-height: 1;
  font-weight: 600;
  color: $grey-10;
}
.title-link {
  color: inherit;
  text-decoration: none;
}
.title-link:hover {
  color: $primary;
}
.status-label {
  width: 140px;
  flex-shrink: 0;
}
.status-value {
  width: 48px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}
.status-bar {
  height: 8px;
  border-radius: 0 4px 4px 0;
  background: #0b63c9;
  min-width: 2px;
  transition: width 0.3s ease;
}
.status-bar--empty {
  background: $grey-4;
}
</style>
