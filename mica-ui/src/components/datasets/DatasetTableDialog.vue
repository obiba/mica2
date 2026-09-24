<template>
  <q-dialog v-model="show" persistent @before-show="onShow">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ t('dataset.study_table') }}</div>
        <div class="text-hint">{{ t('dataset.study_table_info') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll q-gutter-y-sm">
        <q-select
          v-model="studyId"
          :options="studyOptions"
          emit-value
          map-options
          :label="`${t('dataset.study')} *`"
          use-input
          fill-input
          hide-selected
          input-debounce="0"
          dense
          :loading="loadingStudies"
          @filter="onStudyFilter"
          @update:model-value="onStudyChange"
        />
        <div class="row q-col-gutter-md">
          <q-select
            v-model="populationId"
            :options="populationOptions"
            emit-value
            map-options
            :label="`${t('study.population')} *`"
            dense
            class="col-6"
            @update:model-value="dceId = undefined"
          />
          <q-select
            v-model="dceId"
            :options="dceOptions"
            emit-value
            map-options
            :label="`${t('dataset.dce')} *`"
            dense
            class="col-6"
          />
        </div>

        <div class="text-subtitle1 q-mt-md">{{ t('dataset.source.title') }}</div>
        <div class="text-hint">{{ t('dataset.source.info') }}</div>
        <q-option-group
          :model-value="source.namespace"
          :options="SOURCE_NAMESPACES.map((value) => ({ value, label: t(`dataset.source.${value}.title`) }))"
          inline
          dense
          @update:model-value="(namespace) => (source = { namespace })"
        />
        <div class="text-hint">{{ t(`dataset.source.${source.namespace}.info`) }}</div>
        <div v-if="source.namespace === 'opal'" class="row q-col-gutter-md">
          <q-select
            v-model="source.project"
            :options="projectNames"
            :label="`${t('dataset.project')} *`"
            dense
            :loading="loadingProjects"
            class="col-6"
            @update:model-value="source.table = ''"
          />
          <q-select
            v-model="source.table"
            :options="tableNames"
            :label="`${t('dataset.table')} *`"
            dense
            class="col-6"
          />
        </div>
        <div v-else-if="source.namespace === 'file'" class="row q-col-gutter-md">
          <q-input v-model="source.path" :label="`${t('dataset.source.file.path')} *`" dense class="col-6" />
          <q-input v-model="source.table" :label="t('dataset.table')" dense class="col-6" />
        </div>
        <div v-else class="row q-col-gutter-md">
          <q-input v-model="source.nid" :label="`${t('dataset.source.other.nid')} *`" dense class="col-6" />
          <q-input v-model="source.nss" :label="`${t('dataset.source.other.nss')} *`" dense class="col-6" />
        </div>

        <div class="text-subtitle1 q-mt-md">{{ t('dataset.table_labels') }}</div>
        <localized-input v-model="name" :label="t('name')" />
        <localized-input v-model="description" :label="t('description')" :rows="3" />
        <localized-input v-model="additionalInformation" :label="t('dataset.additional_information')" :rows="3" />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn flat :label="t('save')" color="primary" :disable="!complete" v-close-popup @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { api } from 'src/boot/api';
import type { DatasetDto_StudyTableDto, LocalizedStringDto, StudySummaryDto } from 'src/models/Mica';
import type { ProjectDto } from 'src/models/Opal';
import LocalizedInput from 'src/components/commons/LocalizedInput.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { isSourceComplete, SOURCE_NAMESPACES, tableSource, withSource, type TableSource } from 'src/utils/datasets';
import { notifyError } from 'src/utils/notify';
import { localized } from 'src/utils/persons';

interface Props {
  /** the table to edit, a new one when undefined */
  table?: DatasetDto_StudyTableDto | undefined;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ save: [table: DatasetDto_StudyTableDto] }>();
const documentsStore = useDocumentsStore();
const { t, locale } = useI18n();

const studies = ref<StudySummaryDto[]>([]);
const loadingStudies = ref(false);
const studyFilter = ref('');
const projects = ref<ProjectDto[]>([]);
const loadingProjects = ref(false);

const studyId = ref<string>();
const populationId = ref<string>();
const dceId = ref<string>();
const source = ref<TableSource>({ namespace: 'opal' });
const name = ref<LocalizedStringDto[]>([]);
const description = ref<LocalizedStringDto[]>([]);
const additionalInformation = ref<LocalizedStringDto[]>([]);

function option(id: string, label: LocalizedStringDto[]) {
  const text = localized(label, locale.value);
  return { value: id, label: text ? `${id} - ${text}` : id };
}

const study = computed(() => studies.value.find((summary) => summary.id === studyId.value));
const population = computed(() =>
  study.value?.populationSummaries?.find((summary) => summary.id === populationId.value),
);
const studyOptions = computed(() =>
  studies.value
    .map((summary) => option(summary.id, summary.acronym))
    .filter((item) => item.label.toLowerCase().includes(studyFilter.value)),
);
const populationOptions = computed(() =>
  (study.value?.populationSummaries ?? []).map((summary) => option(summary.id, summary.name)),
);
const dceOptions = computed(() =>
  (population.value?.dataCollectionEventSummaries ?? []).map((summary) => option(summary.id, summary.name)),
);
/** the Opal projects of the study, and the one of the table when the Opal is out of reach */
const projectNames = computed(() =>
  withCurrent(
    projects.value.map((project) => project.name),
    source.value.project,
  ),
);
const tableNames = computed(() =>
  withCurrent(
    projects.value.find((project) => project.name === source.value.project)?.datasource?.table ?? [],
    source.value.table,
  ),
);
const complete = computed(
  () => !!studyId.value && !!populationId.value && !!dceId.value && isSourceComplete(source.value),
);

function withCurrent(names: string[], current: string | undefined) {
  return current && !names.includes(current) ? [current, ...names] : names;
}

function onStudyFilter(value: string, update: (callback: () => void) => void) {
  update(() => (studyFilter.value = value.toLowerCase()));
}

async function loadStudies() {
  loadingStudies.value = true;
  try {
    // the individual study states carry the population and event summaries
    studies.value = (await documentsStore.fetchDocuments(documentTarget('individual-study', ''))) as StudySummaryDto[];
  } catch (error) {
    notifyError(error);
  } finally {
    loadingStudies.value = false;
  }
}

/** the Opal projects of the study (its own Opal or the default one), none when Opal is out of reach */
async function loadProjects(id: string | undefined) {
  projects.value = [];
  if (!id) return;
  loadingProjects.value = true;
  try {
    projects.value = (await api.get<ProjectDto[]>(`/draft/study-state/${id}/opal-projects`)).data ?? [];
  } catch (error) {
    notifyError(error);
  } finally {
    loadingProjects.value = false;
  }
}

watch(studyId, loadProjects);

function onStudyChange() {
  populationId.value = undefined;
  dceId.value = undefined;
}

function onShow() {
  studyFilter.value = '';
  studyId.value = props.table?.studyId || undefined;
  populationId.value = props.table?.populationId || undefined;
  dceId.value = props.table?.dataCollectionEventId || undefined;
  source.value = props.table ? tableSource(props.table) : { namespace: 'opal', project: '', table: '' };
  name.value = props.table?.name ?? [];
  description.value = props.table?.description ?? [];
  additionalInformation.value = props.table?.additionalInformation ?? [];
  loadStudies();
}

function onSave() {
  // the study summary and the event unique id are computed by the server
  const table = { ...props.table };
  delete table.studySummary;
  delete table.dceId;
  emit(
    'save',
    withSource(
      {
        ...table,
        studyId: studyId.value ?? '',
        populationId: populationId.value,
        dataCollectionEventId: dceId.value,
        name: name.value,
        description: description.value,
        additionalInformation: additionalInformation.value,
      },
      source.value,
    ),
  );
}
</script>
