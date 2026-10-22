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
      <div class="text-grey-8 q-mb-lg">{{ t('config.logs.info') }}</div>
      <loggers-table />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import LoggersTable from 'src/components/settings/system/LoggersTable.vue';

const { t } = useI18n();
</script>
