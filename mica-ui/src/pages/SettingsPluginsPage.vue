<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.plugins')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="text-h5 q-mb-sm">{{ t('config.plugins.title') }}</div>
      <div class="text-grey-8 q-mb-lg">{{ t('config.plugins.info') }}</div>
      <q-banner v-if="installed?.restart" dense rounded class="bg-orange-1 text-orange-10 q-mb-md">
        <template #avatar><q-icon name="warning" color="warning" /></template>
        {{ t('config.plugins.restart_required') }}
      </q-banner>
      <div class="row">
        <div class="col-12 col-md-10">
          <q-tabs v-model="tab" dense align="left" active-color="primary" indicator-color="primary" class="text-grey-8">
            <q-tab name="installed" :label="t('config.plugins.installed')" />
            <q-tab name="updates" :label="t('config.plugins.updates')" />
            <q-tab name="available" :label="t('config.plugins.available')" />
            <q-tab name="site" :label="t('config.plugins.site')" />
          </q-tabs>
          <q-separator />
          <q-tab-panels v-model="tab" animated>
            <q-tab-panel name="installed" class="q-px-none">
              <q-select
                v-model="type"
                :options="[...PLUGIN_TYPES]"
                :label="t('type')"
                clearable
                dense
                outlined
                class="q-mb-md"
                style="max-width: 220px"
              />
              <q-list v-if="installedPackages.length" bordered separator>
                <plugin-package-item v-for="pkg in installedPackages" :key="pkg.name" :pkg="pkg">
                  <q-spinner v-if="busy === pkg.name" color="primary" size="sm" class="q-mr-sm" />
                  <template v-if="!pkg.uninstalled">
                    <q-btn
                      round
                      flat
                      size="sm"
                      icon="refresh"
                      :title="t('config.plugins.restart')"
                      :disable="busy !== undefined"
                      @click="ask('restart', pkg)"
                    />
                    <q-btn
                      round
                      flat
                      size="sm"
                      icon="edit"
                      :title="t('config.plugins.configure')"
                      :disable="busy !== undefined"
                      @click="onConfigure(pkg)"
                    />
                    <q-btn
                      round
                      flat
                      size="sm"
                      icon="delete"
                      color="negative"
                      :title="t('config.plugins.uninstall')"
                      :disable="busy !== undefined"
                      @click="ask('uninstall', pkg)"
                    />
                  </template>
                  <q-btn
                    v-else
                    round
                    flat
                    size="sm"
                    icon="undo"
                    :title="t('config.plugins.cancel_uninstall')"
                    :disable="busy !== undefined"
                    @click="cancelUninstall(pkg.name)"
                  />
                </plugin-package-item>
              </q-list>
              <div v-else-if="installed" class="text-grey-7">{{ t('config.plugins.none_installed') }}</div>
            </q-tab-panel>
            <q-tab-panel v-for="kind in REPOSITORY_TABS" :key="kind" :name="kind" class="q-px-none">
              <div class="text-grey-8 q-mb-md">{{ t(`config.plugins.${kind}_info`) }}</div>
              <q-spinner-dots v-if="loadingPackages" color="primary" size="md" />
              <q-list v-else-if="packages[kind].length" bordered separator>
                <plugin-package-item v-for="pkg in packages[kind]" :key="`${pkg.name}:${pkg.version}`" :pkg="pkg">
                  <q-btn
                    round
                    flat
                    size="sm"
                    icon="download"
                    color="primary"
                    :title="t('config.plugins.install')"
                    :loading="busy === pkg.name"
                    :disable="busy !== undefined"
                    @click="ask('install', pkg)"
                  />
                </plugin-package-item>
              </q-list>
              <div v-else class="text-grey-7">{{ t(`config.plugins.none_${kind}`) }}</div>
            </q-tab-panel>
            <q-tab-panel name="site" class="q-px-none">
              <div class="text-grey-8 q-mb-md">{{ t('config.plugins.site_info') }}</div>
              <template v-if="installed">
                <a :href="installed.site" target="_blank" rel="noopener">{{ installed.site }}</a>
                <div v-if="installed.updated" class="text-grey-7 q-mt-sm" :title="getDateLabel(installed.updated)">
                  {{ t('config.plugins.last_update', { ago: getDateDistanceLabel(installed.updated) }) }}
                </div>
              </template>
            </q-tab-panel>
          </q-tab-panels>
        </div>
      </div>
    </q-page>
    <plugin-config-dialog
      v-model="showConfig"
      :name="selected?.name || ''"
      :load="getSiteProperties"
      :save="saveSiteProperties"
      @saved="notifySuccess(t('config.plugins.configured'))"
    />
    <confirm-dialog v-model="showConfirm" :title="confirmTitle" :text="confirmText" @confirm="onConfirm" />
  </div>
</template>

<script setup lang="ts">
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import PluginConfigDialog from 'src/components/settings/plugins/PluginConfigDialog.vue';
import PluginPackageItem from 'src/components/settings/plugins/PluginPackageItem.vue';
import { PLUGIN_TYPES, usePlugins } from 'src/composables/usePlugins';
import type { PluginPackageDto } from 'src/models/MicaPlugins';
import { getDateDistanceLabel, getDateLabel } from 'src/utils/dates';
import { notifySuccess } from 'src/utils/notify';

type RepositoryTab = 'updates' | 'available';
type Action = 'install' | 'uninstall' | 'restart';

const REPOSITORY_TABS: RepositoryTab[] = ['updates', 'available'];

const { t } = useI18n();
const {
  installed,
  busy,
  load,
  loadPackages,
  install,
  uninstall,
  cancelUninstall,
  restart,
  getSiteProperties,
  saveSiteProperties,
} = usePlugins();

const tab = ref<'installed' | RepositoryTab | 'site'>('installed');
const type = ref<string | null>(null);
const packages = reactive<Record<RepositoryTab, PluginPackageDto[]>>({ updates: [], available: [] });
const loadingPackages = ref(false);

const installedPackages = computed(() =>
  (installed.value?.packages || []).filter((pkg) => !type.value || pkg.type === type.value),
);

onMounted(load);

watch(tab, async (value) => {
  if (value !== 'updates' && value !== 'available') return;
  loadingPackages.value = true;
  packages[value] = await loadPackages(value);
  loadingPackages.value = false;
});

const selected = ref<PluginPackageDto>();
const showConfig = ref(false);
const showConfirm = ref(false);
const action = ref<Action>();

function onConfigure(pkg: PluginPackageDto) {
  selected.value = pkg;
  showConfig.value = true;
}

function ask(next: Action, pkg: PluginPackageDto) {
  action.value = next;
  selected.value = pkg;
  showConfirm.value = true;
}

const confirmTitle = computed(() => (action.value ? t(`config.plugins.${action.value}_title`) : ''));

const confirmText = computed(() => {
  if (!action.value || !selected.value) return '';
  const { name, version, type: pkgType } = selected.value;
  const text = t(`config.plugins.${action.value}_text`, { name, version });
  // the search engine is down while its plugin restarts, and missing after its uninstallation
  return pkgType === 'mica-search' && action.value !== 'install'
    ? `${text} ${t('config.plugins.search_warning')}`
    : text;
});

async function onConfirm() {
  const pkg = selected.value;
  if (!pkg) return;
  if (action.value === 'install') {
    if (await install(pkg)) {
      packages.updates = packages.updates.filter((p) => p.name !== pkg.name);
      packages.available = packages.available.filter((p) => p.name !== pkg.name);
      notifySuccess(t('config.plugins.installed_text'));
    }
  } else if (action.value === 'uninstall') {
    if (await uninstall(pkg.name)) notifySuccess(t('config.plugins.uninstalled_text'));
  } else if (action.value === 'restart') {
    if (await restart(pkg.name)) notifySuccess(t('config.plugins.restarted'));
  }
}
</script>
