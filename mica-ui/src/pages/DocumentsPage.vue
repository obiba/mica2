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
      <documents-table :target="target" />
      <document-edit-dialog v-model="showNew" :type="type" @saved="(id) => router.push(`${target.routeBase}/${id}`)" />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import DocumentEditDialog from 'src/components/documents/DocumentEditDialog.vue';
import DocumentsTable from 'src/components/documents/DocumentsTable.vue';
import { useDocumentTarget, useRouteDocumentType } from 'src/composables/useDocumentTarget';

const { t } = useI18n();
const type = useRouteDocumentType();
const { target } = useDocumentTarget(type, '');
const router = useRouter();
const showNew = ref(false);
</script>
