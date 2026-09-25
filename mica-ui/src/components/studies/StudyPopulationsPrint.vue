<template>
  <!-- ponytail: the forms are rendered even when not printing, fine for a few populations and events -->
  <div class="print-only">
    <div v-for="population in populations" :key="population.id ?? ''" style="break-before: page">
      <div class="text-h5 q-mb-sm">
        {{ t('study.population') }}: {{ localized(population.name, locale) || population.id }}
      </div>
      <entity-json-form
        :model-value="toModel(population, POPULATION_FIELDS)"
        form-path="/config/population/form"
        readonly
      />
      <div v-for="dce in byWeight(population.dataCollectionEvents)" :key="dce.id ?? ''" class="q-mt-md q-ml-md">
        <div class="text-h6">{{ localized(dce.name, locale) || dce.id }}</div>
        <div class="text-caption text-grey-7 q-mb-sm">{{ dcePeriod(dce, t('study.ongoing')) }}</div>
        <entity-json-form
          :model-value="toModel(dce, DCE_FIELDS)"
          form-path="/config/data-collection-event/form"
          readonly
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { StudyDto } from 'src/models/Mica';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import { DCE_FIELDS, POPULATION_FIELDS, toModel } from 'src/composables/useDocumentModel';
import { localized } from 'src/utils/persons';
import { byWeight, dcePeriod } from 'src/utils/studies';

interface Props {
  study: StudyDto;
}

const props = defineProps<Props>();
const { t, locale } = useI18n();

const populations = computed(() => byWeight(props.study.populations));
</script>
