<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('networks.title')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <q-btn color="primary" icon="add" :label="t('add')" size="sm" to="/network/new" class="q-mb-md" />
      <networks-table />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import NetworksTable from 'src/components/networks/NetworksTable.vue';

const { t } = useI18n();
</script>
