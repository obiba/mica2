<template>
  <config-section :title="t('config.data_source')" :items="items">
    <template #form="{ form }">
      <config-input v-model="form.opal" name="opal" type="url" />
      <config-input v-model="form.privacyThreshold" name="privacy_threshold" type="number" :min="0" />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import type { FieldItem } from 'src/components/FieldsList.vue';
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import { valueItem } from 'src/components/settings/general/fields';
import type { MicaConfigDto } from 'src/models/Mica';

const { t } = useI18n();

const systemStore = useSystemStore();

/** the Opal URL as a link, when set */
const opalItem = computed<FieldItem>(() =>
  systemStore.configuration.opal
    ? {
        field: 'opal',
        label: 'config.opal',
        hint: 'config.opal_help',
        links: (config: MicaConfigDto) => [
          { label: config.opal || '', to: config.opal || '', iconRight: 'open_in_new' },
        ],
      }
    : valueItem('opal', 'opal'),
);

const items = computed(() => [opalItem.value, valueItem('privacyThreshold', 'privacy_threshold')]);
</script>
