<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings_general')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <div class="text-h5 q-mb-md">{{ t('properties') }}</div>
      <general-properties />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import GeneralProperties from 'src/components/settings/general/GeneralProperties.vue';

const { t } = useI18n();
const systemStore = useSystemStore();

onMounted(() => {
  systemStore.init();
});
</script>
