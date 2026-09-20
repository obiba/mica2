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
        <q-tab-panels v-model="tab" animated>
          <q-tab-panel name="view" class="q-pa-none">
            <document-view-panel :target="target" :document="document" />
          </q-tab-panel>
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
import type { EntityStateDto } from 'src/models/Mica';
import DrawerLayout from 'src/components/DrawerLayout.vue';
import DocumentHeader from 'src/components/documents/DocumentHeader.vue';
import DocumentViewPanel from 'src/components/documents/DocumentViewPanel.vue';
import DocumentHistoryPanel from 'src/components/history/DocumentHistoryPanel.vue';
import DocumentAclPanel from 'src/components/permissions/DocumentAclPanel.vue';
import CommentsPanel from 'src/components/comments/CommentsPanel.vue';
import FileBrowser from 'src/components/files/FileBrowser.vue';
import DataAccessRequestLink from 'src/components/projects/DataAccessRequestLink.vue';
import { useDocumentTarget, useRouteDocumentType } from 'src/composables/useDocumentTarget';
import { useDocumentState } from 'src/composables/useDocumentState';
import { useDocumentActions, type DocumentAction } from 'src/composables/useDocumentActions';
import type { DocumentDto } from 'src/stores/documents';

const TABS = ['view', 'history', 'files', 'comments', 'permissions'];

const documentsStore = useDocumentsStore();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const type = useRouteDocumentType();
const id = computed(() => route.params.id as string);
const tab = computed(() => {
  const name = route.params.tab as string | undefined;
  return name && TABS.includes(name) ? name : 'view';
});

const { target } = useDocumentTarget(type, id);
const document = ref<DocumentDto>();
const state = ref<EntityStateDto>();
const { canEdit, canManagePermissions } = useDocumentState(state);
/** the drawer entries, one per tab */
const menu = computed(() => [
  { name: 'view', icon: 'visibility', label: 'view' },
  { name: 'history', icon: 'history', label: 'history.title' },
  { name: 'files', icon: 'folder', label: 'files.title' },
  { name: 'comments', icon: 'comment', label: 'comments.title' },
  ...(canManagePermissions.value ? [{ name: 'permissions', icon: 'lock', label: 'permissions' }] : []),
]);
const { busy, apply } = useDocumentActions(target);
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
