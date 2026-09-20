<template>
  <div>
    <q-page class="q-pa-none column">
      <q-toolbar class="bg-grey-3">
        <q-breadcrumbs>
          <q-breadcrumbs-el icon="home" to="/" />
          <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
          <q-breadcrumbs-el :label="t('settings.network')" />
        </q-breadcrumbs>
      </q-toolbar>

      <drawer-layout class="col">
        <template #drawer>
          <q-list padding role="none">
            <q-item clickable v-ripple :active="tab === 'form'" @click="selectTab('form')">
              <q-item-section avatar>
                <q-icon name="list" />
              </q-item-section>
              <q-item-section>{{ t('config.form') }}</q-item-section>
            </q-item>
            <q-item clickable v-ripple :active="tab === 'permissions'" @click="selectTab('permissions')">
              <q-item-section avatar>
                <q-icon name="lock" />
              </q-item-section>
              <q-item-section>{{ t('permissions') }}</q-item-section>
            </q-item>
          </q-list>
        </template>

        <q-tab-panels v-model="tab" animated>
          <q-tab-panel name="form" class="q-pa-none">
            <div class="text-h5 q-mb-md">{{ t('config.network_form') }}</div>
            <entity-form-builder ref="formBuilder" :target="target" :info="t('config.network_form_info')" />
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
      </drawer-layout>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import DrawerLayout from 'src/components/DrawerLayout.vue';
import EntityFormBuilder from 'src/components/settings/forms/EntityFormBuilder.vue';
import AclPanel from 'src/components/permissions/AclPanel.vue';
import { EntityFormDto_Type } from 'src/models/Mica';
import type { EntityConfigTarget } from 'src/composables/useEntityConfigForm';
import type { AclEndpoints } from 'src/composables/useAcl';

const { t } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();

const tab = ref('form');
const formBuilder = ref<InstanceType<typeof EntityFormBuilder>>();
const target: EntityConfigTarget = { name: 'network', type: EntityFormDto_Type.Network };
/** the permissions on any draft network and the accesses to any published network */
const endpoints: AclEndpoints = { permissions: '/config/network/permissions', accesses: '/config/network/accesses' };

/** switches the panel, unless the form has unsaved changes the user keeps */
async function selectTab(name: string) {
  if (name === tab.value) return;
  if (tab.value === 'form' && formBuilder.value && !(await formBuilder.value.confirmLeave())) return;
  tab.value = name;
}

onMounted(() => {
  systemStore.init();
});
</script>
