<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.translations')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="text-h5 q-mb-sm">{{ t('config.translations.title') }}</div>
      <div class="text-grey-8 q-mb-lg">{{ t('config.translations.info') }}</div>
      <translations-table />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import TranslationsTable from 'src/components/settings/system/TranslationsTable.vue';

const { t } = useI18n();
</script>
