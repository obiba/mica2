<template>
  <q-item>
    <q-item-section top>
      <q-item-label class="text-subtitle1">{{ pkg.title }}</q-item-label>
      <q-item-label lines="1">
        <span class="text-weight-medium">{{ pkg.name }}</span>
        <q-badge outline color="grey-7" class="on-right">{{ pkg.type }}</q-badge>
      </q-item-label>
      <q-item-label caption>{{ pkg.description }}</q-item-label>
      <q-item-label lines="1" class="text-body2 text-primary">
        <a v-if="pkg.website?.startsWith('http')" :href="pkg.website" target="_blank" rel="noopener">{{
          pkg.author
        }}</a>
        <span v-else>{{ pkg.author }}</span>
        <span class="text-grey-7 on-right">{{ pkg.maintainer }}</span>
        <code class="on-right">{{ pkg.license }}</code>
      </q-item-label>
    </q-item-section>
    <q-item-section side>
      <q-item-label caption lines="1">
        <q-icon v-if="pkg.uninstalled" name="close" color="negative" class="on-left" />
        <span :title="`Mica: ${pkg.micaVersion}`">{{ pkg.version }}</span>
      </q-item-label>
    </q-item-section>
    <q-item-section side>
      <div class="row items-center no-wrap">
        <slot />
      </div>
    </q-item-section>
  </q-item>
</template>

<script setup lang="ts">
import type { PluginPackageDto } from 'src/models/MicaPlugins';

defineProps<{ pkg: PluginPackageDto }>();
</script>
