<template>
  <div>
    <q-page class="q-pa-none column">
      <q-toolbar class="bg-grey-3">
        <q-breadcrumbs>
          <q-breadcrumbs-el icon="home" to="/" />
          <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
          <q-breadcrumbs-el :label="t('settings.taxonomies')" />
        </q-breadcrumbs>
      </q-toolbar>

      <drawer-layout class="col">
        <template #drawer>
          <q-list padding role="none">
            <q-item
              v-for="item in MENU"
              :key="item.name"
              clickable
              v-ripple
              :active="tab === item.name"
              :to="tabRoute(item.name)"
            >
              <q-item-section avatar>
                <q-icon :name="item.icon" />
              </q-item-section>
              <q-item-section>{{ t(item.label) }}</q-item-section>
            </q-item>
          </q-list>
        </template>

        <q-tab-panels v-model="tab">
          <q-tab-panel name="browse" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('taxonomies.title') }}</div>
            <div class="text-grey-8 q-mb-lg">{{ t('taxonomies.info') }}</div>
            <taxonomies-browser v-model="nodeKey" />
          </q-tab-panel>
          <q-tab-panel name="files" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('taxonomies.files') }}</div>
            <div class="text-grey-8 q-mb-lg">{{ t('taxonomies.files_info') }}</div>
            <file-browser :root="FILES_ROOT" :path="filePath" @update:path="onFilePath" />
          </q-tab-panel>
        </q-tab-panels>
      </drawer-layout>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import DrawerLayout from 'src/components/DrawerLayout.vue';
import FileBrowser from 'src/components/files/FileBrowser.vue';
import TaxonomiesBrowser from 'src/components/settings/taxonomies/TaxonomiesBrowser.vue';

const FILES_ROOT = '/taxonomies';

const MENU = [
  { name: 'browse', icon: 'account_tree', label: 'taxonomies.title' },
  { name: 'files', icon: 'folder', label: 'taxonomies.files' },
];

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const tab = computed(() => (route.params.tab === 'files' ? 'files' : 'browse'));

function tabRoute(name: string) {
  return name === 'browse' ? '/settings/taxonomies' : `/settings/taxonomies/${name}`;
}

/** the selected tree node, kept in the route */
const nodeKey = computed({
  get: () => (typeof route.query.node === 'string' ? route.query.node : null),
  set: (value) => router.replace({ query: { ...route.query, node: value || undefined } }),
});

/** the folder opened in the files tab, kept in the route */
const filePath = computed(() => (typeof route.query.path === 'string' ? route.query.path : undefined));

function onFilePath(value: string) {
  router.replace({ query: { ...route.query, path: value === FILES_ROOT ? undefined : value } });
}
</script>
