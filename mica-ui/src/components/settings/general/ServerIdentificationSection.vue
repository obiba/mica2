<template>
  <config-section :title="t('config.server_identification')" :items="items">
    <template #form="{ form }">
      <config-input v-model="form.name" name="name" required />
      <config-input v-model="form.publicUrl" name="public_url" type="url" />
      <config-input v-model="form.portalUrl" name="portal_url" type="url" />
      <config-toggle v-model="form.isUsePublicUrlForSharedLink" name="use_public_url_for_shared_link" />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem, valueItem } from 'src/components/settings/general/fields';

const { t } = useI18n();

const items = [
  valueItem('name', 'name'),
  valueItem('publicUrl', 'public_url'),
  valueItem('portalUrl', 'portal_url'),
  flagItem('isUsePublicUrlForSharedLink', 'use_public_url_for_shared_link'),
];
</script>
