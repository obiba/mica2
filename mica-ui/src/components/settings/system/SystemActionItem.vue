<template>
  <q-item class="q-py-md">
    <q-item-section v-if="icon" avatar>
      <q-avatar :icon="icon" color="grey-2" text-color="grey-8" />
    </q-item-section>
    <q-item-section>
      <q-item-label>{{ title }}</q-item-label>
      <q-item-label caption lines="3">{{ help }}</q-item-label>
    </q-item-section>
    <q-item-section side class="text-no-wrap">
      <div class="row items-center q-gutter-xs">
        <q-spinner v-if="busy" color="primary" size="1.5em" class="q-mr-sm" />
        <slot />
      </div>
    </q-item-section>
  </q-item>
</template>

<script setup lang="ts">
/** a row of the caching and indexing pages: a title, a help text and the action buttons */
defineProps<{
  title: string;
  help: string;
  icon?: string;
  /** an action is running on this row */
  busy?: boolean;
}>();
</script>
