<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-4 col-sm-4 col-xs-12">
          <q-list separator>
            <q-item-label header class="text-uppercase">{{ t('settings.system') }}</q-item-label>
            <q-item>
              <q-item-section>
                <q-item-label>
                  <router-link to="/settings/general">{{ t('settings.general') }}</router-link>
                </q-item-label>
                <q-item-label caption lines="2">{{ t('settings.general_caption') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item>
              <q-item-section>
                <q-item-label>
                  <router-link to="/settings/notifications">{{ t('settings.notifications') }}</router-link>
                </q-item-label>
                <q-item-label caption lines="2">{{ t('settings.notifications_caption') }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </div>
        <div class="col-4 col-sm-4 col-xs-12">
          <q-list separator>
            <q-item-label header class="text-uppercase">{{ t('settings.content') }}</q-item-label>
            <q-item v-for="entry in contentEntries" :key="entry.type">
              <q-item-section>
                <q-item-label>
                  <router-link :to="entityConfigRoute(entry.type)">{{ t(entry.label) }}</router-link>
                </q-item-label>
                <q-item-label caption lines="2">{{ t(entry.caption) }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item v-if="systemStore.configuration.isDataAccessEnabled">
              <q-item-section>
                <q-item-label>
                  <router-link to="/settings/data-access">{{ t('settings.data_access') }}</router-link>
                </q-item-label>
                <q-item-label caption lines="2">{{ t('settings.data_access_caption') }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </div>
        <div class="col-4 col-sm-4 col-xs-12">
          <q-list separator>
            <q-item-label header class="text-uppercase">{{ t('settings.search') }}</q-item-label>
          </q-list>
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import { DOCUMENT_TYPES } from 'src/composables/useDocumentTarget';
import { entityConfig, entityConfigRoute } from 'src/utils/entityConfigs';

const { t } = useI18n();
const systemStore = useSystemStore();

/** the settings of the document types whose section is enabled */
const contentEntries = computed(() =>
  DOCUMENT_TYPES.map((type) => ({ type, ...entityConfig(type) })).filter((entry) =>
    entry.isEnabled(systemStore.configuration),
  ),
);

onMounted(() => {
  systemStore.init();
});
</script>
