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
      <div class="row q-col-gutter-lg q-mt-md">
        <div class="col-12 col-md-6">
          <encryption-keys-panel class="q-mb-lg" />
          <opal-credentials-panel />
        </div>
        <div class="col-12 col-md-6">
          <roles-panel />
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import GeneralProperties from 'src/components/settings/general/GeneralProperties.vue';
import EncryptionKeysPanel from 'src/components/settings/general/EncryptionKeysPanel.vue';
import OpalCredentialsPanel from 'src/components/settings/general/OpalCredentialsPanel.vue';
import RolesPanel from 'src/components/settings/general/RolesPanel.vue';

const { t } = useI18n();
const systemStore = useSystemStore();

onMounted(() => {
  systemStore.init();
});
</script>
