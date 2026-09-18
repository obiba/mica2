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
        >
          <template #actions>
            <q-btn v-if="canEdit" color="primary" icon="edit" :label="t('edit')" size="sm" :to="`/network/${id}/edit`" />
          </template>
        </document-header>
        <q-tabs inline-label dense class="text-grey" active-color="primary" indicator-color="primary" align="justify">
          <q-route-tab name="view" icon="visibility" :label="t('view')" :to="tabRoute('view')" exact />
          <q-route-tab name="history" icon="history" :label="t('history.title')" :to="tabRoute('history')" />
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
            <document-history-panel :target="target" :state="network.state" :can-restore="canEdit" @restored="refresh" />
          </q-tab-panel>
          <q-tab-panel name="files">
            {{ t('files.title') }}
          </q-tab-panel>
          <q-tab-panel name="permissions">
            <document-acl-panel :target="target" :can-edit="canManagePermissions" />
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
import DocumentHeader from 'src/components/documents/DocumentHeader.vue';
import DocumentHistoryPanel from 'src/components/history/DocumentHistoryPanel.vue';
import DocumentAclPanel from 'src/components/permissions/DocumentAclPanel.vue';
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
const { canEdit, canManagePermissions } = useDocumentState(() => network.value?.state);
const { busy, apply } = useDocumentActions(target);

const loading = ref(true);

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

watch(id, initialize, { immediate: true });
</script>
