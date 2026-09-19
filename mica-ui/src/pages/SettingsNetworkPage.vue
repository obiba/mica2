<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.network')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <q-tabs v-model="tab" dense align="left" active-color="primary" narrow-indicator class="q-mb-md">
        <q-tab name="form" icon="list" :label="t('config.form')" />
        <q-tab name="permissions" icon="lock" :label="t('permissions')" />
      </q-tabs>
      <q-tab-panels v-model="tab" animated>
        <q-tab-panel name="form" class="q-pa-none">
          <div class="text-h5 q-mb-md">{{ t('config.network_form') }}</div>
          <entity-form-builder :target="target" :info="t('config.network_form_info')" />
        </q-tab-panel>
        <q-tab-panel name="permissions" class="q-pa-none">
          <div class="text-h5 q-mb-md">{{ t('permissions') }}</div>
          <acl-panel
            :endpoints="endpoints"
            :can-edit="authStore.isAdministrator === true"
            :permission-help="t('permission.help_global')"
            :access-help="t('access.help_global')"
            with-file
          />
        </q-tab-panel>
      </q-tab-panels>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import EntityFormBuilder from 'src/components/settings/forms/EntityFormBuilder.vue';
import AclPanel from 'src/components/permissions/AclPanel.vue';
import { EntityFormDto_Type } from 'src/models/Mica';
import type { EntityConfigTarget } from 'src/composables/useEntityConfigForm';
import type { AclEndpoints } from 'src/composables/useAcl';

const { t } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();

const tab = ref('form');
const target: EntityConfigTarget = { name: 'network', type: EntityFormDto_Type.Network };
/** the permissions on any draft network and the accesses to any published network */
const endpoints: AclEndpoints = { permissions: '/config/network/permissions', accesses: '/config/network/accesses' };

onMounted(() => {
  systemStore.init();
});
</script>
