<template>
  <q-card flat bordered class="full-height">
    <q-card-section class="q-pb-none">
      <div class="row items-center no-wrap">
        <q-icon name="bar_chart" size="sm" color="grey-7" class="q-mr-sm" />
        <div class="text-subtitle1 text-weight-medium ellipsis">{{ t('config.statistics.types.DatasetVariable') }}</div>
      </div>
    </q-card-section>
    <q-card-section>
      <div class="row items-baseline no-wrap">
        <a :href="publishedUrl" class="hero-number" :title="t('config.statistics.view_search')">
          {{ formatNumber(published) }}
        </a>
        <div class="q-ml-sm text-grey-7">
          <div class="text-caption text-uppercase">{{ t('config.statistics.published') }}</div>
        </div>
      </div>
      <div class="q-mt-md">
        <div class="row items-center text-caption text-grey-7">
          <span>{{ t('config.statistics.harmonized_share') }}</span>
          <q-space />
          <span class="text-weight-medium text-grey-9">{{ formatNumber(harmonized) }} · {{ share }}%</span>
        </div>
        <q-linear-progress :value="published > 0 ? harmonized / published : 0" rounded size="10px" color="primary" track-color="blue-1" class="q-mt-xs" />
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { portalSearchUrl, type TypeMetrics } from 'src/composables/useContentMetrics';
import { formatNumber } from 'src/utils/numbers';

/** the published variables, and how many of them are harmonized */
const props = defineProps<{
  metrics: TypeMetrics;
}>();

const { t } = useI18n();

const published = computed(() => props.metrics.counts.published ?? 0);
const harmonized = computed(() => props.metrics.counts.harmonized ?? 0);
const share = computed(() => (published.value > 0 ? Math.round((100 * harmonized.value) / published.value) : 0));
const publishedUrl = computed(() => portalSearchUrl('DatasetVariable', 'published'));
</script>

<style scoped lang="scss">
.hero-number {
  font-size: 2.5rem;
  line-height: 1;
  font-weight: 600;
  color: $grey-10;
  text-decoration: none;
}
a.hero-number:hover {
  color: $primary;
}
</style>
