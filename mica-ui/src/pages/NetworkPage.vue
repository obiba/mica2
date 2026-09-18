<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('networks.title')" to="/networks" />
        <q-breadcrumbs-el :label="id" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <q-spinner-dots v-if="loading" color="primary" size="2em" />
      <div v-else-if="network">
        <document-header
          :id="id"
          :timestamps="network.timestamps"
          :state="network.state"
          :disable="busy"
          @action="onAction"
        />
        <q-tabs inline-label dense class="text-grey" active-color="primary" indicator-color="primary" align="justify">
          <q-route-tab name="view" icon="visibility" :label="t('view')" :to="tabRoute('view')" exact />
          <q-route-tab name="history" icon="history" :label="t('history')" :to="tabRoute('history')" />
          <q-route-tab name="files" icon="folder" :label="t('files.title')" :to="tabRoute('files')" />
          <q-route-tab
            v-if="canManagePermissions"
            name="permissions"
            icon="lock"
            :label="t('permissions')"
            :to="tabRoute('permissions')"
          />
        </q-tabs>
        <q-separator />
        <q-tab-panels v-model="tab">
          <q-tab-panel name="view">
            <network-view-panel :network="network" />
          </q-tab-panel>
          <q-tab-panel name="history">
            <q-spinner-dots v-if="loadingCommits" color="primary" size="2em" />
            <pre v-else>{{ commits }}</pre>
          </q-tab-panel>
          <q-tab-panel name="files">
            {{ t('files.title') }}
          </q-tab-panel>
          <q-tab-panel name="permissions">
            {{ t('permissions') }}
          </q-tab-panel>
        </q-tab-panels>
      </div>
      <div v-else>
        {{ t('document.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import type { GitCommitInfoDto } from 'src/models/Mica';
import DocumentHeader from 'src/components/documents/DocumentHeader.vue';
import NetworkViewPanel from 'src/components/networks/NetworkViewPanel.vue';
import { useDocumentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentState } from 'src/composables/useDocumentState';
import { useDocumentActions, type DocumentAction } from 'src/composables/useDocumentActions';

const TABS = ['view', 'history', 'files', 'permissions'];

const networksStore = useNetworksStore();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const id = computed(() => route.params.id as string);
const tab = computed(() => {
  const name = route.params.tab as string | undefined;
  return name && TABS.includes(name) ? name : 'view';
});

const { target } = useDocumentTarget('network', id);
const network = computed(() => networksStore.network);
const { canManagePermissions } = useDocumentState(() => network.value?.state);
const { busy, apply } = useDocumentActions(target);

const loading = ref(true);
const loadingCommits = ref(false);
const commits = ref<GitCommitInfoDto[]>([]);

function tabRoute(name: string) {
  return name === 'view' ? `${target.value.routeBase}/${id.value}` : `${target.value.routeBase}/${id.value}/${name}`;
}

async function initialize() {
  loading.value = true;
  networksStore.network = null;
  try {
    await networksStore.fetchNetwork(id.value);
  } catch (error) {
    console.error('Failed to fetch network:', error);
  } finally {
    loading.value = false;
  }
}

async function refresh() {
  try {
    await networksStore.fetchNetwork(id.value);
  } catch (error) {
    console.error('Failed to fetch network:', error);
  }
}

async function onAction(action: DocumentAction) {
  const result = await apply(action);
  if (result === 'deleted') {
    await router.replace('/networks');
  } else if (result === 'updated') {
    await refresh();
  }
}

async function loadCommits() {
  commits.value = [];
  loadingCommits.value = true;
  try {
    commits.value = await networksStore.fetchNetworkCommits(id.value);
  } catch (error) {
    console.error('Failed to fetch commits:', error);
  } finally {
    loadingCommits.value = false;
  }
}

watch(
  tab,
  (name) => {
    if (name === 'history') loadCommits();
  },
  { immediate: true },
);

watch(id, initialize, { immediate: true });
</script>
