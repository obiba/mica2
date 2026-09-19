<template>
  <config-section :title="t('config.sections')" :items="items">
    <template #form="{ form }">
      <config-toggle v-model="form.isRepositoryEnabled" name="repository_enabled" />
      <q-select
        v-model="form.usableVariableTaxonomiesForConceptTagging"
        :options="taxonomyOptions"
        multiple
        use-chips
        emit-value
        map-options
        dense
        outlined
        :label="t('config.taxonomies_for_concept_tagging')"
        :hint="t('config.taxonomies_for_concept_tagging_help')"
        class="q-mb-md"
      />
      <config-toggle v-model="form.isSingleStudyEnabled" name="single_study_enabled" />
      <config-toggle v-model="form.isNetworkEnabled" name="network_enabled" />
      <config-toggle
        v-model="form.isSingleNetworkEnabled"
        name="single_network_enabled"
        :disable="!form.isNetworkEnabled"
      />
      <config-toggle v-model="form.isCollectedDatasetEnabled" name="collected_dataset_enabled" />
      <config-toggle v-model="form.isHarmonizedDatasetEnabled" name="harmonized_dataset_enabled" />
      <config-toggle v-model="form.isProjectEnabled" name="project_enabled" />
      <config-toggle v-model="form.isDataAccessEnabled" name="data_access_enabled" />
      <config-toggle v-model="form.isImportStudiesFeatureEnabled" name="import_studies_feature_enabled" />
      <config-toggle v-model="form.isCommentsRequiredOnDocumentSave" name="comments_required" />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem, formattedItem } from 'src/components/settings/general/fields';
import type { MicaConfigDto } from 'src/models/Mica';
import { localeText } from 'src/utils/config';

const { t, locale } = useI18n();
const systemStore = useSystemStore();

/** a taxonomy can only be flagged as missing once the Opal taxonomies are known */
const summaryLoaded = ref(false);

/** the taxonomy titles by name, in the locale of the UI */
const taxonomyTitles = computed<Record<string, string>>(() =>
  Object.fromEntries(
    systemStore.taxonomiesSummary.map((summary) => [
      summary.name,
      localeText(summary.title, locale.value, summary.name),
    ]),
  ),
);

/** a configured taxonomy that Opal does not serve anymore is still listed, flagged */
function taxonomyLabel(name: string) {
  const title = taxonomyTitles.value[name];
  if (title) return title;
  return summaryLoaded.value ? `${name} (${t('config.taxonomies_for_concept_tagging_missing')})` : name;
}

const taxonomyOptions = computed(() => {
  const names = new Set([
    ...Object.keys(taxonomyTitles.value),
    ...(systemStore.configuration.usableVariableTaxonomiesForConceptTagging || []),
  ]);
  return [...names].map((name) => ({ label: taxonomyLabel(name), value: name }));
});

const items = computed(() => [
  flagItem('isRepositoryEnabled', 'repository_enabled'),
  formattedItem(
    'usableVariableTaxonomiesForConceptTagging',
    'taxonomies_for_concept_tagging',
    (config: MicaConfigDto) =>
      (config.usableVariableTaxonomiesForConceptTagging || []).map(taxonomyLabel).join(', ') || '-',
  ),
  flagItem('isSingleStudyEnabled', 'single_study_enabled'),
  flagItem('isNetworkEnabled', 'network_enabled'),
  flagItem('isSingleNetworkEnabled', 'single_network_enabled'),
  flagItem('isCollectedDatasetEnabled', 'collected_dataset_enabled'),
  flagItem('isHarmonizedDatasetEnabled', 'harmonized_dataset_enabled'),
  flagItem('isProjectEnabled', 'project_enabled'),
  flagItem('isDataAccessEnabled', 'data_access_enabled'),
  flagItem('isImportStudiesFeatureEnabled', 'import_studies_feature_enabled'),
  flagItem('isCommentsRequiredOnDocumentSave', 'comments_required'),
]);

onMounted(() => {
  systemStore
    .loadTaxonomiesSummary()
    .then(() => {
      summaryLoaded.value = true;
    })
    .catch(() => {
      // Opal may be unreachable: the taxonomies are then shown by name
    });
});
</script>
