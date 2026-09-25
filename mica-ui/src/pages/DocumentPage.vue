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
            <template v-for="item in menu" :key="item.name">
              <q-separator v-if="item.name === 'history'" />
              <q-item
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
            </template>
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
            <q-btn
              v-if="canChangeStatus"
              outline
              color="primary"
              icon="share"
              :label="t('document.share')"
              size="sm"
              @click="showShare = true"
            />
            <q-btn
              v-if="type.endsWith('-study')"
              outline
              color="primary"
              icon="download"
              :label="t('document.export_csv')"
              size="sm"
              type="a"
              :href="toServerUrl(`${target.path}/export_csv`)"
              target="_blank"
            />
            <q-btn outline color="primary" icon="print" :label="t('document.print')" size="sm" @click="print" />
          </template>
        </document-header>
        <q-tab-panels v-model="tab">
          <q-tab-panel name="view" class="q-pa-none">
            <q-banner v-if="dataset && state?.requireIndexing" dense rounded class="bg-warning text-white q-mb-md">
              <template #avatar><q-icon name="warning" /></template>
              {{ t('dataset.require_indexing') }}
              <template #action>
                <q-btn flat dense :label="t('dataset.go_indexing')" to="/settings/indexing" />
              </template>
            </q-banner>
            <document-view-panel :target="target" :document="document" />
            <div v-if="study && hasEvents" class="q-mt-lg">
              <div class="text-h6 q-mb-sm">{{ t('study.timeline') }}</div>
              <study-timeline
                :populations="study.populations"
                @select="(event) => onPopulationSelect(event.populationId, event.dceId)"
              />
            </div>
            <study-populations-print v-if="study" :study="study" />
          </q-tab-panel>
          <q-tab-panel v-if="study" name="populations" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('study.populations') }}</div>
            <q-separator class="q-mb-md" />
            <study-populations-panel
              :study="study"
              :population-id="queryValue('population')"
              :dce-id="queryValue('dce')"
              :can-edit="canEdit"
              :busy="saving"
              @change="onDocumentChange"
              @select="onPopulationSelect"
            />
          </q-tab-panel>
          <q-tab-panel v-if="dataset" name="tables" class="q-pa-none">
            <div v-if="type === 'collected-dataset'">
              <div class="text-h5 q-mb-sm">{{ t('dataset.table') }}</div>
              <q-separator class="q-mb-md" />
              <collected-table-panel
                :dataset="dataset"
                :can-edit="canEdit"
                :busy="saving"
                @change="onDocumentChange"
                @refresh="refresh"
              />
            </div>
            <div v-else>
              <div class="text-h5 q-mb-sm">{{ t('dataset.tables') }}</div>
              <q-separator class="q-mb-md" />
              <harmonized-tables-panel
                :dataset="dataset"
                :can-edit="canEdit"
                :busy="saving"
                @change="onDocumentChange"
            />
            </div>
          </q-tab-panel>
          <template v-if="network">
            <q-tab-panel name="studies" class="q-pa-none">
              <div v-for="(kind, index) in STUDY_KINDS" :key="kind" :class="{ 'q-mt-lg': index > 0 }">
                <div class="text-h5 q-mb-sm">{{ t(documentTarget(kind, '').labels.title) }}</div>
                <q-separator class="q-mb-md" />
                <network-links-panel
                  :network="network"
                  :kind="kind"
                  :can-edit="canEdit"
                  :busy="saving"
                  @change="onDocumentChange"
                />
              </div>
            </q-tab-panel>
            <q-tab-panel name="networks" class="q-pa-none">
              <div class="text-h5 q-mb-sm">{{ t('networks.title') }}</div>
              <q-separator class="q-mb-md" />
              <network-links-panel
                :network="network"
                kind="network"
                :can-edit="canEdit"
                :busy="saving"
                @change="onDocumentChange"
              />
            </q-tab-panel>
          </template>
          <q-tab-panel v-if="membersParent && membersDocument" name="members" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('members.title') }}</div>
            <q-separator class="q-mb-md" />
            <members-panel
              :document="membersDocument"
              :parent="membersParent"
              :can-edit="canEdit"
              :busy="saving"
              @change="onDocumentChange"
            />
          </q-tab-panel>
          <q-tab-panel name="history" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('history.title') }}</div>
            <q-separator class="q-mb-md" />
            <document-history-panel :target="target" :state="state" :can-restore="canEdit" @restored="refresh" />
            <q-separator class="q-mb-md" />
          </q-tab-panel>
          <q-tab-panel name="files" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('files.title') }}</div>
            <q-separator class="q-mb-md" />
            <file-browser :root="target.filesPath" :path="filePath" @update:path="onFilePath" />
          </q-tab-panel>
          <q-tab-panel name="comments" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('comments.title') }}</div>
            <q-separator class="q-mb-md" />
            <comments-panel :path="target.path" />
          </q-tab-panel>
          <q-tab-panel name="permissions" class="q-pa-none">
            <div class="text-h5 q-mb-sm">{{ t('permissions') }}</div>
            <q-separator class="q-mb-md" />
            <document-acl-panel :target="target" :can-edit="canManagePermissions" />
          </q-tab-panel>
        </q-tab-panels>
        <share-document-dialog v-model="showShare" :target="target" />
      </drawer-layout>
      <div v-else class="q-pa-md">
        {{ t('document.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import { useQuasar } from 'quasar';
import type { DatasetDto, EntityStateDto, NetworkDto, StudyDto } from 'src/models/Mica';
import DrawerLayout from 'src/components/DrawerLayout.vue';
import DocumentHeader from 'src/components/documents/DocumentHeader.vue';
import ShareDocumentDialog from 'src/components/documents/ShareDocumentDialog.vue';
import DocumentViewPanel from 'src/components/documents/DocumentViewPanel.vue';
import DocumentHistoryPanel from 'src/components/history/DocumentHistoryPanel.vue';
import DocumentAclPanel from 'src/components/permissions/DocumentAclPanel.vue';
import CommentsPanel from 'src/components/comments/CommentsPanel.vue';
import FileBrowser from 'src/components/files/FileBrowser.vue';
import DataAccessRequestLink from 'src/components/projects/DataAccessRequestLink.vue';
import NetworkLinksPanel from 'src/components/networks/NetworkLinksPanel.vue';
import MembersPanel from 'src/components/persons/MembersPanel.vue';
import StudyPopulationsPanel from 'src/components/studies/StudyPopulationsPanel.vue';
import StudyPopulationsPrint from 'src/components/studies/StudyPopulationsPrint.vue';
import StudyTimeline from 'src/components/studies/StudyTimeline.vue';
import CollectedTablePanel from 'src/components/datasets/CollectedTablePanel.vue';
import HarmonizedTablesPanel from 'src/components/datasets/HarmonizedTablesPanel.vue';
import {
  documentTarget,
  useDocumentTarget,
  useRouteDocumentType,
  type DocumentType,
} from 'src/composables/useDocumentTarget';
import { useDocumentState } from 'src/composables/useDocumentState';
import {
  conflictError,
  notifyPotentialConflicts,
  useDocumentActions,
  type DocumentAction,
} from 'src/composables/useDocumentActions';
import type { DocumentDto } from 'src/stores/documents';
import type { NetworkLinkKind } from 'src/utils/networks';
import type { MembersParent } from 'src/utils/persons';
import { notifyError } from 'src/utils/notify';
import { toServerUrl } from 'src/boot/api';

const TABS = ['view', 'history', 'files', 'comments', 'permissions'];
/** the sections of the studies tab of a network */
const STUDY_KINDS: NetworkLinkKind[] = ['individual-study', 'harmonization-study'];
const NETWORK_TABS = ['studies', 'networks'];
const DATASET_TYPES: DocumentType[] = ['collected-dataset', 'harmonized-dataset'];
/** the documents with members, by the kind of their memberships */
const MEMBERS_PARENTS: Partial<Record<DocumentType, MembersParent>> = {
  network: 'network',
  'individual-study': 'study',
  'harmonization-study': 'study',
};

const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const $q = useQuasar();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const type = useRouteDocumentType();
const membersParent = computed(() => MEMBERS_PARENTS[type.value]);
const id = computed(() => route.params.id as string);
const tab = computed(() => {
  const name = route.params.tab as string | undefined;
  const tabs = [
    ...TABS,
    ...(type.value === 'network' ? NETWORK_TABS : []),
    ...(type.value === 'individual-study' ? ['populations'] : []),
    ...(DATASET_TYPES.includes(type.value) ? ['tables'] : []),
    ...(membersParent.value ? ['members'] : []),
  ];
  return name && tabs.includes(name) ? name : 'view';
});

const { target } = useDocumentTarget(type, id);
const document = ref<DocumentDto>();
const state = ref<EntityStateDto>();
/** the document when it has members */
const membersDocument = computed(() =>
  membersParent.value ? (document.value as NetworkDto | StudyDto | undefined) : undefined,
);
/** the document when it is an individual study (with populations) */
const study = computed(() =>
  type.value === 'individual-study' ? (document.value as StudyDto | undefined) : undefined,
);
/** the document when it is a dataset (with tables) */
const dataset = computed(() =>
  DATASET_TYPES.includes(type.value) ? (document.value as DatasetDto | undefined) : undefined,
);
const hasEvents = computed(() =>
  (study.value?.populations ?? []).some((population) => (population.dataCollectionEvents ?? []).length > 0),
);
const { canEdit, canChangeStatus, canManagePermissions } = useDocumentState(state);
/** the drawer entries, one per tab */
const menu = computed(() => [
  { name: 'view', icon: 'visibility', label: 'view' },
  ...(network.value
    ? [
        { name: 'studies', icon: 'book', label: 'network_links.studies' },
        { name: 'networks', icon: 'hub', label: 'networks.title' },
      ]
    : []),
  ...(study.value ? [{ name: 'populations', icon: 'groups', label: 'study.populations' }] : []),
  ...(dataset.value
    ? [
        {
          name: 'tables',
          icon: 'table_chart',
          label: type.value === 'collected-dataset' ? 'dataset.table' : 'dataset.tables',
        },
      ]
    : []),
  ...(membersParent.value ? [{ name: 'members', icon: 'people', label: 'members.title' }] : []),
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
const showShare = ref(false);

function print() {
  window.print();
}

/** the folder opened in the files tab, kept in the route */
const filePath = computed(() => (typeof route.query.path === 'string' ? route.query.path : undefined));

function onFilePath(value: string) {
  router.replace({ query: { ...route.query, path: value === target.value.filesPath ? undefined : value } });
}

function queryValue(name: string) {
  const value = route.query[name];
  return typeof value === 'string' ? value : undefined;
}

/** shows a population in the populations tab, with one of its events opened */
function onPopulationSelect(populationId: string, dceId?: string) {
  const query = { population: populationId, ...(dceId ? { dce: dceId } : {}) };
  if (tab.value === 'populations') router.replace({ query });
  else router.push({ path: tabRoute('populations'), query });
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
  showShare.value = false;
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

/** saves the document changed in a tab (network links, members order, study populations) */
async function onDocumentChange(dto: DocumentDto, params: Record<string, boolean> = {}) {
  let comment: string | undefined;
  if (systemStore.configuration.isCommentsRequiredOnDocumentSave === true) {
    comment = await promptComment();
    if (comment === undefined) return;
  }
  saving.value = true;
  try {
    notifyPotentialConflicts(await documentsStore.saveDocument(target.value, dto, comment, params));
    await refresh();
  } catch (error) {
    notifyError(study.value ? conflictError(error, 'study.population_conflict') : error);
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
