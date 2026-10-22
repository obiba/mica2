<template>
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
import { useFormsStore } from 'src/stores/forms';

interface Props {
  study: StudyDto;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  /** every form is rendered, the page can be printed */
  ready: [];
  /** a form cannot be read (already notified) */
  error: [];
}>();
const { t, locale } = useI18n();
const formsStore = useFormsStore();

const populations = computed(() => byWeight(props.study.populations));

// the forms are requested by the children first (shared requests): they are converted by the time
// these resolve, rendered on the next tick
onMounted(async () => {
  try {
    await Promise.all([
      formsStore.getForm('/config/population/form', locale.value),
      formsStore.getForm('/config/data-collection-event/form', locale.value),
    ]);
    await nextTick();
    emit('ready');
  } catch {
    emit('error');
  }
});
</script>
