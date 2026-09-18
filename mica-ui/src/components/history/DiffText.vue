<template>
  <span class="diff-text">
    <template v-for="(part, index) in parts" :key="index">
      <del v-if="part.removed" class="text-negative">{{ part.value }}</del>
      <ins v-else-if="part.added" class="text-positive">{{ part.value }}</ins>
      <span v-else>{{ part.value }}</span>
    </template>
  </span>
</template>

<script setup lang="ts">
import { diffWords } from 'diff';

interface Props {
  /** the older text */
  from: unknown;
  /** the newer text, whose changes are highlighted */
  to: unknown;
}

const props = defineProps<Props>();

function asText(value: unknown): string {
  return value === undefined || value === null ? '' : String(value);
}

const parts = computed(() => diffWords(asText(props.from), asText(props.to)));
</script>

<style scoped>
.diff-text del {
  text-decoration: line-through;
}
.diff-text ins {
  text-decoration: none;
  font-weight: 500;
}
</style>
