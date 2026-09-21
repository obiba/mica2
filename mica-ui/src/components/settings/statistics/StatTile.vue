<template>
  <div class="stat-tile">
    <div class="text-caption text-grey-7 ellipsis">{{ label }}</div>
    <div class="text-subtitle1 text-weight-medium">
      <a v-if="href" :href="href" class="stat-link" :title="t('config.statistics.view_search')">{{ formatted }}</a>
      <router-link v-else-if="to" :to="to" class="stat-link" :title="t('config.statistics.view_list')">{{
        formatted
      }}</router-link>
      <span v-else>{{ formatted }}</span>
      <span v-if="secondary !== undefined" class="text-caption text-grey-7 q-ml-xs">/ {{ formatNumber(secondary) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatNumber } from 'src/utils/numbers';

/** a small labelled number, linking to the list of what it counts when there is one */
const props = defineProps<{
  label: string;
  value: number;
  /** a second number shown after the value (`published / total`) */
  secondary?: number | undefined;
  /** an in-app route */
  to?: string | undefined;
  /** an external page (portal search) */
  href?: string | undefined;
}>();

const { t } = useI18n();

const formatted = computed(() => formatNumber(props.value));
</script>

<style scoped lang="scss">
.stat-tile {
  min-width: 90px;
}
.stat-link {
  color: inherit;
  text-decoration: none;
  border-bottom: 1px dotted $grey-6;
}
.stat-link:hover {
  color: $primary;
  border-bottom-color: $primary;
}
</style>
