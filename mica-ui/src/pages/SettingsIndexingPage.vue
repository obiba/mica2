<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.indexing')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="text-h5 q-mb-sm">{{ t('config.indexing.title') }}</div>
      <div class="text-grey-8 q-mb-lg">{{ t('config.indexing.info') }}</div>
      <div class="row">
        <div class="col-12 col-md-8">
          <indices-panel />
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import IndicesPanel from 'src/components/settings/system/IndicesPanel.vue';

const { t } = useI18n();
const systemStore = useSystemStore();

onMounted(() => {
  systemStore.init();
});
</script>
