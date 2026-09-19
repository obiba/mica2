<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-6 col-sm-6 col-xs-12">
          <q-list separator>
            <q-item-label header class="text-uppercase">{{ t('settings.system') }}</q-item-label>
            <q-item>
              <q-item-section>
                <q-item-label>
                  <router-link to="/settings/general">{{ t('settings.general') }}</router-link>
                </q-item-label>
                <q-item-label caption lines="2">{{ t('settings.general_caption') }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </div>
        <div class="col-6 col-sm-6 col-xs-12">
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n();
</script>
