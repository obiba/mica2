<template>
  <div>
    <q-page class="q-pa-none column">
      <q-toolbar class="bg-grey-3">
        <q-breadcrumbs>
          <q-breadcrumbs-el icon="home" to="/" />
          <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
          <q-breadcrumbs-el :label="t('settings.data_access')" />
        </q-breadcrumbs>
      </q-toolbar>

      <drawer-layout class="col">
        <template #drawer>
          <q-list padding role="none">
            <q-item
              v-for="form in forms"
              :key="form.kind"
              clickable
              v-ripple
              :active="tab === form.kind"
              @click="selectTab(form.kind)"
            >
              <q-item-section avatar>
                <q-icon :name="form.icon" />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ t(form.label) }}</q-item-label>
                <q-item-label v-if="!form.enabled" caption>{{ t('config.data_access.form_disabled') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-separator />
            <q-item clickable v-ripple :active="tab === 'notifications'" @click="selectTab('notifications')">
              <q-item-section avatar>
                <q-icon name="mail" />
              </q-item-section>
              <q-item-section>{{ t('config.data_access.notifications') }}</q-item-section>
            </q-item>
            <q-item clickable v-ripple :active="tab === 'settings'" @click="selectTab('settings')">
              <q-item-section avatar>
                <q-icon name="settings" />
              </q-item-section>
              <q-item-section>{{ t('config.data_access.other_settings') }}</q-item-section>
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
          <q-tab-panel v-for="form in forms" :key="form.kind" :name="form.kind" class="q-pa-none">
            <div class="text-h5 q-mb-md">{{ t(form.label) }}</div>
            <config-form-builder
              ref="formBuilders"
              :state="form.state"
              :info="t('config.data_access.form_info', { form: t(form.label).toLowerCase() })"
              :legacy-message="t('config.form_legacy')"
            />
            <template v-if="form.kind === 'application'">
              <data-access-form-properties :state="applicationForm" :info="t('config.data_access.properties_info')" />
              <data-access-pdf-templates :state="applicationForm" />
            </template>
            <data-access-form-properties
              v-else-if="form.kind === 'amendment'"
              :state="amendmentForm"
              :info="t('config.data_access.amendment_properties_info')"
              prefix="amendment_"
            />
          </q-tab-panel>
          <q-tab-panel name="notifications" class="q-pa-none">
            <div class="text-h5 q-mb-md">{{ t('config.data_access.notifications') }}</div>
            <data-access-notifications-panel ref="notificationsPanel" :state="dataAccessConfig" />
          </q-tab-panel>
          <q-tab-panel name="settings" class="q-pa-none">
            <div class="text-h5 q-mb-md">{{ t('config.data_access.other_settings') }}</div>
            <data-access-settings-panel ref="settingsPanel" :state="dataAccessConfig" />
          </q-tab-panel>
          <q-tab-panel name="permissions" class="q-pa-none">
            <div class="text-h5 q-mb-md">{{ t('permissions') }}</div>
            <config-acl-panel
              endpoint="/config/data-access-form/permissions"
              :title="t('config.data_access.permissions_title')"
              :help="t('config.data_access.permissions_help')"
              :roles="readerRole"
              :file-label="t('permission.file_permission')"
              :other-resources="otherResources"
              :can-edit="authStore.isAdministrator === true"
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
import DataAccessFormProperties from 'src/components/settings/dataaccess/DataAccessFormProperties.vue';
import DataAccessPdfTemplates from 'src/components/settings/dataaccess/DataAccessPdfTemplates.vue';
import DataAccessNotificationsPanel from 'src/components/settings/dataaccess/DataAccessNotificationsPanel.vue';
import DataAccessSettingsPanel from 'src/components/settings/dataaccess/DataAccessSettingsPanel.vue';
import ConfigAclPanel from 'src/components/permissions/ConfigAclPanel.vue';
import { useDataAccessConfig } from 'src/composables/useDataAccessConfig';
import { useGuardedTab } from 'src/composables/useGuardedTab';
import {
  DATA_ACCESS_FORM_KINDS,
  useDataAccessConfigForm,
  type DataAccessFormKind,
} from 'src/composables/useDataAccessConfigForm';
import type { DataAccessConfigDto } from 'src/models/Mica';

const { t } = useI18n();
const systemStore = useSystemStore();
const authStore = useAuthStore();

/** the one role on the data access requests */
const readerRole = computed(() => [
  { value: 'READER', label: t('permission.reader'), help: t('config.data_access.reader_help') },
]);
/** the other resources a permission can be granted on too */
const otherResources = computed(() => [
  { value: 'action-logs', label: t('config.data_access.action_logs_permission') },
  { value: 'private-comment', label: t('config.data_access.private_comment_permission') },
]);
/** the data access configuration: the enabled forms here, the notifications and other settings in their panels */
const dataAccessConfig = useDataAccessConfig();

/** the icon of each form in the drawer, as in the legacy administration page */
const ICONS: Record<DataAccessFormKind, string> = {
  application: 'list',
  preliminary: 'play_circle',
  feasibility: 'help',
  amendment: 'edit_note',
  agreement: 'gavel',
};

/** whether a form is enabled in the other settings (the application form always is) */
const ENABLED: Record<DataAccessFormKind, (config: DataAccessConfigDto) => boolean> = {
  application: () => true,
  preliminary: (config) => config.preliminaryEnabled,
  feasibility: (config) => config.feasibilityEnabled,
  amendment: (config) => config.amendmentsEnabled,
  agreement: (config) => config.agreementEnabled,
};

const applicationForm = useDataAccessConfigForm('application');
const amendmentForm = useDataAccessConfigForm('amendment');
/** the five forms with their edition state */
const states = {
  application: applicationForm,
  preliminary: useDataAccessConfigForm('preliminary'),
  feasibility: useDataAccessConfigForm('feasibility'),
  amendment: amendmentForm,
  agreement: useDataAccessConfigForm('agreement'),
};
const forms = computed(() =>
  DATA_ACCESS_FORM_KINDS.map((kind) => ({
    kind,
    label: `config.data_access.${kind}_form`,
    icon: ICONS[kind],
    state: states[kind],
    enabled: dataAccessConfig.config.value ? ENABLED[kind](dataAccessConfig.config.value) : true,
  })),
);

/** the mounted form builders: the one of the active panel */
const formBuilders = ref<InstanceType<typeof ConfigFormBuilder>[]>([]);
const notificationsPanel = ref<InstanceType<typeof DataAccessNotificationsPanel>>();
const settingsPanel = ref<InstanceType<typeof DataAccessSettingsPanel>>();
/** the editors of the active panel guard the switch with their unsaved changes */
const { tab, selectTab } = useGuardedTab('application', () => [
  ...formBuilders.value,
  notificationsPanel.value,
  settingsPanel.value,
]);

onMounted(() => {
  systemStore.init();
  dataAccessConfig.load();
});
</script>
