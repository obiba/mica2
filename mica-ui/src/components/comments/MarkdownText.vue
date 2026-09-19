<template>
  <q-markdown :src="text" no-heading-anchor-links no-html class="markdown-text" />
</template>

<script setup lang="ts">
import { QMarkdown } from '@quasar/quasar-ui-qmarkdown';

interface Props {
  /** markdown source, rendered without raw HTML */
  text: string;
}

defineProps<Props>();
</script>

<style lang="scss">
/* the markdown of a comment or a description: headings kept close to the text size (QMarkdown forces its own) */
.markdown-text {
  p {
    margin-bottom: 0.5em;
  }
  p:last-child {
    margin-bottom: 0;
  }
  .q-markdown--heading {
    font-size: 1.1em !important;
    line-height: 1.5 !important;
    padding: 0;
    margin: 0.5em 0;
    font-weight: 500;
  }
}
</style>
