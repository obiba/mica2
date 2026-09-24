<template>
  <q-card flat bordered :class="['metrics-card full-height', color]">
    <q-card-section class="q-pb-none">
      <div class="row items-center no-wrap">
        <q-icon :name="icon" size="sm" color="grey-7" class="q-mr-sm" />
        <div class="text-subtitle1 text-weight-medium ellipsis">
          <router-link v-if="allRoute" :to="allRoute" class="title-link">{{ title }}</router-link>
          <span v-else>{{ title }}</span>
        </div>
        <q-space />
        <index-health-chip
          v-if="!hideIndexHealth && metrics.notIndexed !== undefined"
          :count="metrics.notIndexed"
          @click="emit('index', metrics.type)"
        />
      </div>
    </q-card-section>
    <q-card-section>
      <div class="row items-baseline no-wrap">
        <a v-if="publishedUrl" :href="publishedUrl" class="hero-number" :title="t('config.statistics.view_search')">
          {{ formatNumber(published) }}
        </a>
        <span v-else class="hero-number">{{ formatNumber(published) }}</span>
        <div class="q-ml-sm text-grey-7">
          <div class="text-caption text-uppercase">{{ t('config.statistics.published') }}</div>
          <div class="text-caption">{{ t('config.statistics.of_total', { total: formatNumber(total) }) }}</div>
        </div>
      </div>
      <publication-bar
        :total="total"
        :published="published"
        :in-edition="counts.in_edition ?? 0"
        :list-route="listRoute"
        class="q-mt-md"
      />
      <div class="row q-gutter-xs q-mt-md">
        <q-chip
          v-for="chip in statusChips"
          :key="chip.key"
          dense
          outline
          square
          :color="chip.value > 0 ? 'grey-9' : 'grey-6'"
          :icon="chip.icon"
          :clickable="chip.to !== undefined"
          @click="chip.to && router.push(chip.to)"
        >
          {{ t(chip.label) }}
          <span class="text-weight-medium q-ml-xs">{{ formatNumber(chip.value) }}</span>
        </q-chip>
      </div>
      <q-separator class="q-my-md" />
      <div class="row q-col-gutter-md">
        <stat-tile
          v-if="'totalFiles' in counts"
          :label="t('config.statistics.files')"
          :value="counts.publishedFiles ?? 0"
          :secondary="counts.totalFiles ?? 0"
          class="col"
        />
        <stat-tile
          v-if="'variables' in counts"
          :label="t('config.statistics.variables')"
          :value="counts.variables ?? 0"
          :href="portalSearchUrl(metrics.type, 'variables')"
          class="col"
        />
        <stat-tile
          v-if="'totalWithVariable' in counts"
          :label="t('config.statistics.studies_with_variables')"
          :value="counts.totalWithVariable ?? 0"
          :href="portalSearchUrl(metrics.type, 'totalWithVariable')"
          class="col"
        />
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import IndexHealthChip from 'src/components/settings/statistics/IndexHealthChip.vue';
import PublicationBar from 'src/components/settings/statistics/PublicationBar.vue';
import StatTile from 'src/components/settings/statistics/StatTile.vue';
import {
  documentsListRoute,
  portalSearchUrl,
  type MetricsType,
  type TypeMetrics,
} from 'src/composables/useContentMetrics';
import type { StateFilter } from 'src/composables/useDocumentState';
import { formatNumber } from 'src/utils/numbers';

/** the metrics of a document type: publication share, revision statuses, files, variables, index health */
const props = defineProps<{
  metrics: TypeMetrics;
  /** hide the index health chip (outside of the statistics settings page) */
  hideIndexHealth?: boolean;
}>();

const emit = defineEmits<{
  /** the index health chip was clicked: open the indexing dialog of the type */
  (e: 'index', type: MetricsType): void;
}>();

const { t } = useI18n();
const router = useRouter();

const ICONS: Partial<Record<MetricsType, string>> = {
  Network: 'hub',
  Study: 'science',
  HarmonizationStudy: 'join_inner',
  StudyDataset: 'table_chart',
  HarmonizationDataset: 'table_view',
  Project: 'work',
};

/** light background by entity: networks blue, studies green, datasets yellow */
const COLORS: Partial<Record<MetricsType, string>> = {
  Network: 'bg-light-blue-2',
  Study: 'bg-light-green-2',
  HarmonizationStudy: 'bg-light-green-2',
  StudyDataset: 'bg-yellow-2',
  HarmonizationDataset: 'bg-yellow-2',
};

const counts = computed(() => props.metrics.counts);
const total = computed(() => counts.value.total ?? 0);
const published = computed(() => counts.value.published ?? 0);
const color = computed(() => COLORS[props.metrics.type]);
const icon = computed(() => ICONS[props.metrics.type] ?? 'folder');
const title = computed(() => t(`config.statistics.types.${props.metrics.type}`));

const listRoute = (filter: StateFilter) => documentsListRoute(props.metrics.type, filter);
const allRoute = computed(() => documentsListRoute(props.metrics.type));
const publishedUrl = computed(() => portalSearchUrl(props.metrics.type, 'published'));

const statusChips = computed(() => [
  {
    key: 'under_review',
    label: 'config.statistics.under_review',
    icon: 'rate_review',
    value: counts.value.under_review ?? 0,
    to: listRoute('UNDER_REVIEW'),
  },
  {
    key: 'to_delete',
    label: 'config.statistics.to_delete',
    icon: 'delete_outline',
    value: counts.value.to_delete ?? 0,
    to: listRoute('TO_DELETE'),
  },
]);
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
.title-link {
  color: inherit;
  text-decoration: none;
}
.title-link:hover {
  color: $primary;
}
</style>
