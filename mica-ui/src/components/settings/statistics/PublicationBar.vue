<template>
  <div>
    <div class="publication-bar" role="img" :aria-label="ariaLabel">
      <div
        v-for="segment in segments"
        :key="segment.key"
        class="publication-segment"
        :class="`publication-segment--${segment.key}`"
        :style="{ flexGrow: segment.value }"
        :title="`${t(segment.label)}: ${formatNumber(segment.value)}`"
      ></div>
    </div>
    <div class="row q-gutter-x-md q-gutter-y-xs q-mt-xs">
      <div v-for="segment in segments" :key="segment.key" class="row items-center no-wrap text-caption">
        <span class="publication-swatch" :class="`publication-segment--${segment.key}`"></span>
        <router-link v-if="segment.to" :to="segment.to" class="legend-link" :title="t('config.statistics.view_list')">
          {{ t(segment.label) }}
          <span class="text-weight-medium">{{ formatNumber(segment.value) }}</span>
        </router-link>
        <span v-else>
          {{ t(segment.label) }}
          <span class="text-weight-medium">{{ formatNumber(segment.value) }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { StateFilter } from 'src/composables/useDocumentState';
import { formatNumber } from 'src/utils/numbers';

/**
 * The share of the documents of a type that are published: up to date, published with a draft
 * ahead, or not published. The three parts partition the total.
 */
const props = defineProps<{
  total: number;
  published: number;
  /** published documents with revisions ahead */
  inEdition: number;
  /** the route of the documents list filtered by state, when the type has a list page */
  listRoute?: ((filter: StateFilter) => string | undefined) | undefined;
}>();

const { t } = useI18n();

const segments = computed(() => {
  const inEdition = Math.min(props.inEdition, props.published);
  const upToDate = props.published - inEdition;
  const notPublished = Math.max(0, props.total - props.published);
  return [
    { key: 'up-to-date', label: 'config.statistics.up_to_date', value: upToDate, to: props.listRoute?.('PUBLISHED') },
    { key: 'in-edition', label: 'config.statistics.in_edition', value: inEdition, to: props.listRoute?.('IN_EDITION') },
    {
      key: 'not-published',
      label: 'config.statistics.not_published',
      value: notPublished,
      to: props.listRoute?.('NOT_PUBLISHED'),
    },
  ];
});

const ariaLabel = computed(() => segments.value.map((s) => `${t(s.label)}: ${s.value}`).join(', '));
</script>

<style scoped lang="scss">
// one blue ramp: the darker the step, the more published; the track is the lightest step
$up-to-date: #0b63c9;
$in-edition: #7fb8f0;
$not-published: #dce9f7;

.publication-bar {
  display: flex;
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
  gap: 2px; // the surface gap between the segments
  background: transparent;
}
.publication-segment {
  flex-basis: 0;
  min-width: 0;
  transition: flex-grow 0.3s ease;
}
.publication-segment--up-to-date {
  background: $up-to-date;
}
.publication-segment--in-edition {
  background: $in-edition;
}
.publication-segment--not-published {
  background: $not-published;
}
.publication-swatch {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 6px;
  flex-shrink: 0;
}
.legend-link {
  color: inherit;
  text-decoration: none;
}
.legend-link:hover {
  color: $primary;
}
</style>
