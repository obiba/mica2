<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t(target.labels.title)" :to="target.listRoute" />
        <q-breadcrumbs-el :label="id" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <q-spinner-dots v-if="loading" color="primary" size="2em" />
      <div v-else-if="document">
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
        <q-tabs inline-label dense class="text-grey" active-color="primary" indicator-color="primary" align="justify">
          <q-route-tab name="view" icon="visibility" :label="t('view')" :to="tabRoute('view')" exact />
          <q-route-tab name="history" icon="history" :label="t('history.title')" :to="tabRoute('history')" />
          <q-route-tab name="files" icon="folder" :label="t('files.title')" :to="tabRoute('files')" />
          <q-route-tab name="comments" icon="comment" :label="t('comments.title')" :to="tabRoute('comments')" />
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
            <document-view-panel :target="target" :document="document" />
          </q-tab-panel>
          <q-tab-panel name="history">
            <document-history-panel :target="target" :state="state" :can-restore="canEdit" @restored="refresh" />
          </q-tab-panel>
          <q-tab-panel name="files">
            <file-browser :root="target.filesPath" :path="filePath" @update:path="onFilePath" />
          </q-tab-panel>
          <q-tab-panel name="comments">
            <comments-panel :path="target.path" />
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
import type { EntityStateDto } from 'src/models/Mica';
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
