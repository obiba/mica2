<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.caching')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="text-h5 q-mb-sm">{{ t('config.caching.title') }}</div>
      <div class="text-grey-8 q-mb-lg">{{ t('config.caching.info') }}</div>
      <div class="row">
        <div class="col-12 col-md-8">
          <caches-panel />
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import CachesPanel from 'src/components/settings/system/CachesPanel.vue';

const { t } = useI18n();
</script>
