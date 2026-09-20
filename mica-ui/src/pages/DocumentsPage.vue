<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t(target.labels.title)" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-btn color="primary" icon="add" :label="t('add')" size="sm" :to="`${target.routeBase}/new`" class="q-mb-md" />
      <documents-table :target="target" />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import DocumentsTable from 'src/components/documents/DocumentsTable.vue';
import { useDocumentTarget, useRouteDocumentType } from 'src/composables/useDocumentTarget';

const { t } = useI18n();
const type = useRouteDocumentType();
const { target } = useDocumentTarget(type, '');
</script>
