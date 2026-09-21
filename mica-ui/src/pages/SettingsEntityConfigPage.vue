<template>
  <div>
    <q-page class="q-pa-none column">
      <q-toolbar class="bg-grey-3">
        <q-breadcrumbs>
          <q-breadcrumbs-el icon="home" to="/" />
          <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
          <q-breadcrumbs-el :label="t(config.label)" />
        </q-breadcrumbs>
      </q-toolbar>

      <drawer-layout class="col">
        <template #drawer>
          <q-list padding role="none">
            <q-item
              v-for="form in config.forms"
              :key="form.target.name"
              clickable
              v-ripple
              :active="tab === form.target.name"
              @click="selectTab(form.target.name)"
            >
              <q-item-section avatar>
                <q-icon :name="form.icon" />
              </q-item-section>
              <q-item-section>{{ config.forms.length > 1 ? t(form.label) : t('config.form') }}</q-item-section>
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
          <q-tab-panel v-for="form in forms" :key="form.target.name" :name="form.target.name" class="q-pa-none">
            <div class="text-h5 q-mb-md">{{ t(form.label) }}</div>
            <config-form-builder
              ref="formBuilders"
              :state="form.state"
              :info="t('config.form_info', { type: t(form.info.type), fields: t(form.info.fields) })"
            />
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
import ConfigFormBuilder from 'src/components/settings/forms/ConfigFormBuilder.vue';
import AclPanel from 'src/components/permissions/AclPanel.vue';
import { useRouteDocumentType } from 'src/composables/useDocumentTarget';
import { useEntityConfigForm } from 'src/composables/useEntityConfigForm';
import type { AclEndpoints } from 'src/composables/useAcl';
import { entityConfig } from 'src/utils/entityConfigs';

const { t } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();

const documentType = useRouteDocumentType();
/** the forms of the document type and the permissions on its documents */
const config = computed(() => entityConfig(documentType.value));
/** the forms with their edition state, renewed with the document type */
const forms = computed(() => config.value.forms.map((form) => ({ ...form, state: useEntityConfigForm(form.target) })));

const tab = ref(firstTab());
/** the mounted form builders: the one of the active panel */
const formBuilders = ref<InstanceType<typeof ConfigFormBuilder>[]>([]);

/** the permissions on any draft document and the accesses to any published document of the type */
const endpoints = computed<AclEndpoints>(() => ({
  permissions: `/config/${config.value.permissions}/permissions`,
  accesses: `/config/${config.value.permissions}/accesses`,
}));

function firstTab(): string {
  return config.value.forms[0]?.target.name ?? 'permissions';
}

/** switches the panel, unless the form has unsaved changes the user keeps */
async function selectTab(name: string) {
  if (name === tab.value) return;
  for (const builder of formBuilders.value) {
    if (!(await builder.confirmLeave())) return;
  }
  tab.value = name;
}

// the same page serves every document type
watch(documentType, () => {
  tab.value = firstTab();
});

onMounted(() => {
  systemStore.init();
});
</script>
