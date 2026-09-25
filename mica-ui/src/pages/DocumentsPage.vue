<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t(target.labels.title)" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-btn color="primary" icon="add" :label="t('add')" size="sm" class="q-mb-md" @click="showNew = true" />
      <q-btn
        v-if="importType"
        color="secondary"
        icon="upload"
        :label="t('studies_import.import')"
        size="sm"
        class="q-mb-md q-ml-sm"
        @click="showImport = true"
      />
      <documents-table ref="table" :target="target" />
      <document-edit-dialog v-model="showNew" :type="type" @saved="(id) => router.push(`${target.routeBase}/${id}`)" />
      <studies-import-dialog v-if="importType" v-model="showImport" :type="importType" @imported="table?.load()" />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import DocumentEditDialog from 'src/components/documents/DocumentEditDialog.vue';
import DocumentsTable from 'src/components/documents/DocumentsTable.vue';
import StudiesImportDialog from 'src/components/studies/StudiesImportDialog.vue';
import { useDocumentTarget, useRouteDocumentType } from 'src/composables/useDocumentTarget';

const { t } = useI18n();
const type = useRouteDocumentType();
const { target } = useDocumentTarget(type, '');
const router = useRouter();
const showNew = ref(false);
const systemStore = useSystemStore();
const table = ref<InstanceType<typeof DocumentsTable>>();
const showImport = ref(false);
/** the studies can be imported from a remote Mica, when the feature is enabled */
const importType = computed(() =>
  (type.value === 'individual-study' || type.value === 'harmonization-study') &&
  systemStore.configuration.isImportStudiesFeatureEnabled
    ? type.value
    : undefined,
);
// the page is reused from a studies type to the other (history navigation)
watch(type, () => (showImport.value = false));
</script>
