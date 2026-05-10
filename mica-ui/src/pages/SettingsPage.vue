<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <div class="text-h6 q-mb-md">{{ t('properties') }}</div>
      <system-properties />
      <div class="row q-mt-md">
        <div class="col-md-6 col-sm-12">
          <div class="text-h6">
            {{ t('system.translations.title') }}
          </div>
          <div class="text-help">
            {{ t('system.translations.hint') }}
          </div>
          <system-custom-translations />
        </div>
        <div class="col-md-6 col-sm-12">
          <div class="text-h6">
            {{ t('system.attributes.title') }}
          </div>
          <div class="text-help">
            {{ t('system.attributes.hint') }}
          </div>
          <system-user-attributes />
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import SystemProperties from 'src/components/SystemProperties.vue';
import SystemUserAttributes from 'src/components/SystemUserAttributes.vue';
import SystemCustomTranslations from 'src/components/SystemCustomTranslations.vue';

const systemStore = useSystemStore();
const { t } = useI18n();

onMounted(() => {
  systemStore.init();
});
</script>
