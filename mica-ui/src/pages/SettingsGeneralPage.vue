<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.general')" />
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
          <roles-panel class="q-mb-lg" />
          <div class="text-h6 q-mb-sm">{{ t('permissions') }}</div>
          <config-acl-panel
            endpoint="/config/document-sets/permissions"
            :title="t('config.opal_views_permissions')"
            :help="t('config.opal_views_permissions_help')"
            :roles="opalViewsRoles"
            :can-edit="authStore.isAdministrator"
            class="q-mb-md"
          />
          <config-acl-panel
            endpoint="/config/contingencies/permissions"
            :title="t('config.crosstabs_permissions')"
            :help="t('config.crosstabs_permissions_help')"
            :roles="crosstabsRoles"
            :can-edit="authStore.isAdministrator"
          />
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
import ConfigAclPanel from 'src/components/permissions/ConfigAclPanel.vue';
import type { RoleOption } from 'src/components/permissions/AclDialog.vue';

const { t } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();

/** the download of Opal views from the variable lists */
const opalViewsRoles = computed<RoleOption[]>(() => [
  { value: 'READER', label: t('permission.reader'), help: t('config.opal_views_permission_help') },
]);
/** the contingency table analyses */
const crosstabsRoles = computed<RoleOption[]>(() => [
  { value: 'ANALYST', label: t('permission.analyst'), help: t('config.crosstabs_permission_help') },
]);

onMounted(() => {
  systemStore.init();
});
</script>
