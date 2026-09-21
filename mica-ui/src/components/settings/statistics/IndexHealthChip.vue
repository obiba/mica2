<template>
  <q-chip
    dense
    square
    :outline="count === 0"
    :color="count === 0 ? 'positive' : 'negative'"
    :text-color="count === 0 ? 'positive' : 'white'"
    :icon="count === 0 ? 'check_circle' : 'error'"
    :clickable="count > 0"
    @click="count > 0 && emit('click')"
  >
    {{ count === 0 ? t('config.statistics.indexed') : t('config.statistics.not_indexed', count) }}
    <q-tooltip v-if="count > 0">{{ t('config.statistics.not_indexed_hint') }}</q-tooltip>
  </q-chip>
</template>

<script setup lang="ts">
/** the health of the search index of a type: complete, or the count of published documents missing from it */
defineProps<{
  count: number;
}>();

const emit = defineEmits<{
  (e: 'click'): void;
}>();

const { t } = useI18n();
</script>
