<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.logs')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="text-h5 q-mb-sm">{{ t('config.logs.title') }}</div>
      <p class="text-grey-8">{{ t('config.logs.info') }}</p>
      <loggers-table />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import LoggersTable from 'src/components/settings/system/LoggersTable.vue';

const { t } = useI18n();
</script>
