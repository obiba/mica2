<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <div class="row">
        <q-card flat bordered class="on-left q-mb-md o-card-md bg-grey-1">
          <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
            <router-link to="/settings/general">{{ t('settings_general') }}</router-link>
          </q-card-section>
          <q-separator />
          <q-card-section class="text-hint">
            {{ t('settings_general_caption') }}
            <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" to="/settings/general" />
          </q-card-section>
        </q-card>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n();
</script>
