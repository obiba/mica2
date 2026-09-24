<template>
  <div>
    <q-page class="q-pa-none column">
      <q-toolbar class="bg-grey-3">
        <q-breadcrumbs>
          <q-breadcrumbs-el icon="home" to="/" />
          <q-breadcrumbs-el :label="t(target.labels.title)" :to="target.listRoute" />
          <q-breadcrumbs-el :label="id" />
        </q-breadcrumbs>
      </q-toolbar>

      <q-spinner-dots v-if="loading" color="primary" size="2em" class="q-ma-md" />
      <drawer-layout v-else-if="document" class="col">
        <template #drawer>
          <q-list padding role="none">
            <q-item
              v-for="item in menu"
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

        <document-header :id="id" :timestamps="document.timestamps" :state="state" :disable="busy" @action="onAction">
          <template v-if="request" #info>
            <div class="text-caption text-grey-7 q-mt-xs">
              {{ t('data_access_request.title') }}: <data-access-request-link :request="request" />
            </div>
          </template>
          <template #actions>
            <q-btn
              v-if="canEdit"
              color="primary"
              icon="edit"
              :label="t('edit')"
              size="sm"
              :to="`${target.routeBase}/${id}/edit`"
            />
          </template>
        </document-header>
        <q-tab-panels v-model="tab">
          <q-tab-panel name="view" class="q-pa-none">
            <document-view-panel :target="target" :document="document" />
          </q-tab-panel>
          <template v-if="network">
            <q-tab-panel v-for="kind in NETWORK_LINK_KINDS" :key="kind" :name="NETWORK_TAB[kind]" class="q-pa-none">
              <network-links-panel
                :network="network"
                :kind="kind"
                :can-edit="canEdit"
                :busy="saving"
                @change="onNetworkChange"
              />
            </q-tab-panel>
            <q-tab-panel name="members" class="q-pa-none">
              <network-members-panel :network="network" :can-edit="canEdit" :busy="saving" @change="onNetworkChange" />
            </q-tab-panel>
          </template>
          <q-tab-panel name="history" class="q-pa-none">
            <document-history-panel :target="target" :state="state" :can-restore="canEdit" @restored="refresh" />
          </q-tab-panel>
          <q-tab-panel name="files" class="q-pa-none">
            <file-browser :root="target.filesPath" :path="filePath" @update:path="onFilePath" />
          </q-tab-panel>
          <q-tab-panel name="comments" class="q-pa-none">
            <comments-panel :path="target.path" />
          </q-tab-panel>
          <q-tab-panel name="permissions" class="q-pa-none">
            <document-acl-panel :target="target" :can-edit="canManagePermissions" />
          </q-tab-panel>
        </q-tab-panels>
      </drawer-layout>
      <div v-else class="q-pa-md">
        {{ t('document.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import { useQuasar } from 'quasar';
import type { EntityStateDto, NetworkDto } from 'src/models/Mica';
import DrawerLayout from 'src/components/DrawerLayout.vue';
import DocumentHeader from 'src/components/documents/DocumentHeader.vue';
import DocumentViewPanel from 'src/components/documents/DocumentViewPanel.vue';
import DocumentHistoryPanel from 'src/components/history/DocumentHistoryPanel.vue';
import DocumentAclPanel from 'src/components/permissions/DocumentAclPanel.vue';
import CommentsPanel from 'src/components/comments/CommentsPanel.vue';
import FileBrowser from 'src/components/files/FileBrowser.vue';
import DataAccessRequestLink from 'src/components/projects/DataAccessRequestLink.vue';
import NetworkLinksPanel from 'src/components/networks/NetworkLinksPanel.vue';
import NetworkMembersPanel from 'src/components/networks/NetworkMembersPanel.vue';
import { useDocumentTarget, useRouteDocumentType } from 'src/composables/useDocumentTarget';
import { useDocumentState } from 'src/composables/useDocumentState';
import { useDocumentActions, type DocumentAction } from 'src/composables/useDocumentActions';
import type { DocumentDto } from 'src/stores/documents';
import type { NetworkLinkKind } from 'src/utils/networks';
import { notifyError } from 'src/utils/notify';

const TABS = ['view', 'history', 'files', 'comments', 'permissions'];
const NETWORK_LINK_KINDS: NetworkLinkKind[] = ['individual-study', 'harmonization-study', 'network'];
/** the tab of the entities linked to a network */
const NETWORK_TAB: Record<NetworkLinkKind, string> = {
  'individual-study': 'studies',
  'harmonization-study': 'initiatives',
  network: 'networks',
};
const NETWORK_TABS = [...Object.values(NETWORK_TAB), 'members'];

const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const $q = useQuasar();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const type = useRouteDocumentType();
const id = computed(() => route.params.id as string);
const tab = computed(() => {
  const name = route.params.tab as string | undefined;
  const tabs = type.value === 'network' ? [...TABS, ...NETWORK_TABS] : TABS;
  return name && tabs.includes(name) ? name : 'view';
});

const { target } = useDocumentTarget(type, id);
const document = ref<DocumentDto>();
const state = ref<EntityStateDto>();
const { canEdit, canManagePermissions } = useDocumentState(state);
/** the drawer entries, one per tab */
const menu = computed(() => [
  { name: 'view', icon: 'visibility', label: 'view' },
  ...(network.value
    ? [
        { name: 'studies', icon: 'book', label: 'individual.studies.title' },
        { name: 'initiatives', icon: 'book', label: 'harmonization.studies.title' },
        { name: 'networks', icon: 'hub', label: 'networks.title' },
        { name: 'members', icon: 'people', label: 'network_members.title' },
      ]
    : []),
  { name: 'history', icon: 'history', label: 'history.title' },
  { name: 'files', icon: 'folder', label: 'files.title' },
  { name: 'comments', icon: 'comment', label: 'comments.title' },
  ...(canManagePermissions.value ? [{ name: 'permissions', icon: 'lock', label: 'permissions' }] : []),
]);
const { busy, apply } = useDocumentActions(target);
/** the document when it is a network */
const network = computed(() => (type.value === 'network' ? (document.value as NetworkDto | undefined) : undefined));
const saving = ref(false);
/** the data access request a research project comes from */
const request = computed(() => (document.value && 'request' in document.value ? document.value.request : undefined));

const loading = ref(true);

/** the folder opened in the files tab, kept in the route */
const filePath = computed(() => (typeof route.query.path === 'string' ? route.query.path : undefined));

function onFilePath(value: string) {
  router.replace({ query: { ...route.query, path: value === target.value.filesPath ? undefined : value } });
}

function tabRoute(name: string) {
  return name === 'view' ? `${target.value.routeBase}/${id.value}` : `${target.value.routeBase}/${id.value}/${name}`;
}

async function refresh() {
  try {
    const loaded = await documentsStore.fetchDocument(target.value);
    document.value = loaded.document;
    state.value = loaded.state;
  } catch (error) {
    console.error('Failed to fetch document:', error);
  }
}

async function initialize() {
  loading.value = true;
  document.value = undefined;
  state.value = undefined;
  await refresh();
  loading.value = false;
}

/** the revision comment, undefined when cancelled */
function promptComment() {
  return new Promise<string | undefined>((resolve) => {
    $q.dialog({
      title: t('document.comment'),
      message: t('document.comment_hint'),
      prompt: { model: '', isValid: (value: string) => value.trim() !== '', type: 'text' },
      cancel: true,
      persistent: true,
    })
      .onOk((value: string) => resolve(value.trim()))
      .onCancel(() => resolve(undefined));
  });
}

/** saves the network changed in a tab (links, members order) */
async function onNetworkChange(dto: NetworkDto) {
  let comment: string | undefined;
  if (systemStore.configuration.isCommentsRequiredOnDocumentSave === true) {
    comment = await promptComment();
    if (comment === undefined) return;
  }
  saving.value = true;
  try {
    await documentsStore.saveDocument(target.value, dto, comment);
    await refresh();
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}

async function onAction(action: DocumentAction) {
  const result = await apply(action);
  if (result === 'deleted') {
    await router.replace(target.value.listRoute);
  } else if (result === 'updated') {
    await refresh();
  }
}

watch(() => target.value.path, initialize, { immediate: true });
</script>
