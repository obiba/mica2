<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-8 col-sm-8 col-xs-12">
          <q-item-label header class="text-uppercase">{{ t('content_management') }}</q-item-label>
          <div class="row">
            <q-card flat bordered class="on-left q-mb-md o-card-sm bg-grey-1">
              <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
                <router-link to="/networks">{{ t('networks.title') }}</router-link>
              </q-card-section>
              <q-separator />
              <q-card-section class="text-hint">
                {{ t('networks.caption') }}
                <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" :to="`/networks`" />
              </q-card-section>
            </q-card>
            <q-card flat bordered class="on-left q-mb-md o-card-sm bg-grey-1">
              <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
                <router-link to="/studies">{{ t('studies.title') }}</router-link>
              </q-card-section>
              <q-separator />
              <q-card-section class="text-hint">
                {{ t('studies.caption') }}
                <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" :to="`/studies`" />
              </q-card-section>
            </q-card>
            <q-card flat bordered class="on-left q-mb-md o-card-sm bg-grey-1">
              <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
                <router-link to="/datasets">{{ t('datasets.title') }}</router-link>
              </q-card-section>
              <q-separator />
              <q-card-section class="text-hint">
                {{ t('datasets.caption') }}
                <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" :to="`/datasets`" />
              </q-card-section>
            </q-card>
            <q-card flat bordered class="on-left q-mb-md o-card-sm bg-grey-1">
              <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
                <router-link to="/research-projects">{{ t('research_projects.title') }}</router-link>
              </q-card-section>
              <q-separator />
              <q-card-section class="text-hint">
                {{ t('research_projects.caption') }}
                <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" :to="`/research-projects`" />
              </q-card-section>
            </q-card>
            <q-card flat bordered class="on-left q-mb-md o-card-sm bg-grey-1">
              <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
                <router-link to="/files">{{ t('files.title') }}</router-link>
              </q-card-section>
              <q-separator />
              <q-card-section class="text-hint">
                {{ t('files.caption') }}
                <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" :to="`/files`" />
              </q-card-section>
            </q-card>
            <q-card flat bordered class="on-left q-mb-md o-card-sm bg-grey-1">
              <q-card-section class="q-pa-sm text-h6 text-center bg-grey-4">
                <router-link to="/persons">{{ t('persons.title') }}</router-link>
              </q-card-section>
              <q-separator />
              <q-card-section class="text-hint">
                {{ t('persons.caption') }}
                <q-btn flat rounded dense icon="arrow_forward" size="xs" color="primary" :to="`/persons`" />
              </q-card-section>
            </q-card>
          </div>
        </div>
        <div class="col-4 col-sm-4 col-xs-12"></div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
const { t } = useI18n();
//const authStore = useAuthStore();
</script>
