<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('networks.title')" to="/networks" />
        <q-breadcrumbs-el :label="id" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page class="q-pa-md">
      <q-spinner-dots v-if="refreshing" color="primary" size="2em" />
      <div v-else-if="networksStore.network">
        <q-tabs
          v-model="tab"
          inline-label
          dense
          class="text-grey"
          active-color="primary"
          indicator-color="primary"
          align="justify"
          @update:model-value="onTabChanged"
        >
          <q-tab name="view" icon="visibility" :label="t('view')" />
          <q-tab name="diff" icon="difference" :label="t('history')" />
          <q-tab name="files" icon="library_books" :label="t('files')" />
          <q-tab name="permissions" icon="lock" :label="t('permissions')" />
        </q-tabs>
        <q-separator />
        <q-tab-panels v-model="tab">
          <q-tab-panel name="view">
            <div class="row">
              <div class="col-12 col-md-4">
                <q-img
                  v-if="networksStore.network.logo"
                  :src="
                    toServerUrl(
                      `/draft/network/${networksStore.network.id}/file/${networksStore.network.logo.id}/_download`,
                    )
                  "
                  class="q-mb-md"
                  style="max-width: 200px"
                />
              </div>
              <div class="col-12 col-md-8">
                <localized-input v-model="networksStore.network.acronym" :label="t('acronym')" readonly />
                <localized-input v-model="networksStore.network.name" :label="t('name')" readonly />
              </div>
            </div>
            <localized-input
              v-model="networksStore.network.description"
              :label="t('description')"
              :rows="10"
              readonly
            />
            <pre>{{ networksStore.network }}</pre>
          </q-tab-panel>
          <q-tab-panel name="diff">
            <q-spinner-dots v-if="loading" color="primary" size="2em" />
            <pre>{{ commits }}</pre>
          </q-tab-panel>
          <q-tab-panel name="files">
            {{ t('networks.files') }}
          </q-tab-panel>
          <q-tab-panel name="permissions">
            {{ t('networks.permissions') }}
          </q-tab-panel>
        </q-tab-panels>
      </div>
      <div v-else>
        {{ t('networks.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import type { GitCommitInfoDto } from 'src/models/Mica';
import { toServerUrl } from 'src/boot/api';
import LocalizedInput from 'src/components/commons/LocalizedInput.vue';

const networksStore = useNetworksStore();
const router = useRouter();
const { t } = useI18n();

const id = router.currentRoute.value.params.id as string;
const refreshing = ref(true);
const loading = ref(true);
const tab = ref('view');
const commits = ref<GitCommitInfoDto[]>([]);

onMounted(initialize);

async function initialize() {
  try {
    refreshing.value = true;
    await networksStore.fetchNetwork(id);
  } finally {
    refreshing.value = false;
  }
}

async function onTabChanged(newTab: string) {
  commits.value = [];
  loading.value = true;
  try {
    if (newTab === 'diff') {
      commits.value = await networksStore.fetchNetworkCommits(id);
    }
  } catch (error) {
    console.error('Failed to fetch commits:', error);
  } finally {
    loading.value = false;
  }
}
</script>
