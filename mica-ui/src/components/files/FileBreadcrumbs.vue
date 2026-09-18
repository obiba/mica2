<template>
  <q-breadcrumbs class="text-body2" active-color="primary">
    <q-breadcrumbs-el
      v-for="(crumb, index) in breadcrumbs"
      :key="crumb.path"
      :label="crumb.name === '/' ? undefined : crumb.name"
      :icon="crumb.name === '/' ? 'storage' : index === breadcrumbs.length - 1 ? icon : undefined"
      :class="index < breadcrumbs.length - 1 ? 'cursor-pointer' : 'text-weight-medium'"
      @click="index < breadcrumbs.length - 1 && emit('navigate', crumb.path)"
    />
  </q-breadcrumbs>
</template>

<script setup lang="ts">
import type { Breadcrumb } from 'src/utils/files';

interface Props {
  breadcrumbs: Breadcrumb[];
  /** icon of the current document */
  icon?: string;
}

defineProps<Props>();
const emit = defineEmits<{ navigate: [path: string] }>();
</script>
