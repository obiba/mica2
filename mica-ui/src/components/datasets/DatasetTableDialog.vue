<template>
  <q-dialog v-model="show" persistent @before-show="onShow">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ t(TITLES[mode]) }}</div>
        <div class="text-hint">{{ t(`${TITLES[mode]}_info`) }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll q-gutter-y-sm">
        <q-option-group
          v-if="mode === 'harmonized'"
          :model-value="initiative"
          :options="[
            { value: false, label: t('dataset.study') },
            { value: true, label: t('dataset.initiative') },
          ]"
          inline
          dense
          @update:model-value="onKindChange"
        />
        <q-select
          v-model="studyId"
          :options="studyOptions"
          emit-value
          map-options
          :label="`${t(initiative ? 'dataset.initiative' : 'dataset.study')} *`"
          use-input
          fill-input
          hide-selected
          input-debounce="0"
          dense
          :loading="loadingStudies"
          @filter="onStudyFilter"
          @update:model-value="onStudyChange"
        />
        <div v-if="!initiative" class="row q-col-gutter-md q-mt-sm">
          <q-select
            v-model="populationId"
            :options="populationOptions"
            emit-value
            map-options
            :label="t('study.population') + required"
            :clearable="mode === 'harmonized'"
            dense
            class="col-6"
            @update:model-value="dceId = undefined"
          />
          <q-select
            v-model="dceId"
            :options="dceOptions"
            emit-value
            map-options
            :label="t('dataset.dce') + required"
            :clearable="mode === 'harmonized'"
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
          <q-input v-model="source.path" :label="`${t('dataset.source.file.path')} *`" dense class="col-6">
            <template v-slot:append>
              <q-btn
                flat
                dense
                round
                size="sm"
                icon="folder_open"
                :title="t('files.select_file')"
                @click="showFiles = true"
              />
            </template>
          </q-input>
          <q-input v-model="source.table" :label="t('dataset.table')" dense class="col-6" />
        </div>
        <div v-else class="row q-col-gutter-md">
          <q-input v-model="source.nid" :label="`${t('dataset.source.other.nid')} *`" dense class="col-6" />
          <q-input v-model="source.nss" :label="`${t('dataset.source.other.nss')} *`" dense class="col-6" />
        </div>

        <template v-if="mode !== 'schema'">
          <div class="text-subtitle1 q-mt-md">{{ t('dataset.table_labels') }}</div>
          <q-json-form
            v-model="labels"
            :schema="labelsSchema"
            :uischema="labelsForm.uischema"
            :languages="systemStore.languages"
            :validation-mode="validationMode"
            @update:errors="labelErrors = $event"
          />
        </template>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn :label="t('save')" color="primary" :disable="!complete" @click="onSave" />
      </q-card-actions>
    </q-card>
    <file-select-dialog
      v-model="showFiles"
      :folder="filesFolder"
      :extensions="['.xlsx']"
      @select="(path) => (source.path = path)"
    />
  </q-dialog>
</template>

<script setup lang="ts">
import { api } from 'src/boot/api';
import FileSelectDialog from 'src/components/files/FileSelectDialog.vue';
import type { DatasetDto_StudyTableDto, LocalizedStringDto, StudySummaryDto } from 'src/models/Mica';
import type { ProjectDto } from 'src/models/Opal';
import type { ErrorObject } from 'ajv';
import type { ValidationMode } from '@jsonforms/core';
import { QJsonForm, toJsonForms } from '@obiba/quasar-ui-json-form';
import { localizedToArray, localizedToObject, type FormModel } from 'src/composables/useDocumentModel';
import { documentTarget } from 'src/composables/useDocumentTarget';
import {
  isSourceComplete,
  SOURCE_NAMESPACES,
  tableSource,
  withSource,
  type DatasetTable,
  type TableSource,
} from 'src/utils/datasets';
import { parentPath } from 'src/utils/files';
import { notifyError } from 'src/utils/notify';
import { localized } from 'src/utils/persons';

/**
 * `collected`: the study table of a collected dataset (population and event required);
 * `harmonized`: a table of a harmonized dataset, of an individual study (population and event optional)
 * or of a harmonization initiative; `schema`: the data schema of a harmonized dataset (initiative, no labels).
 */
type Mode = 'collected' | 'harmonized' | 'schema';

const TITLES: Record<Mode, string> = {
  collected: 'dataset.study_table',
  harmonized: 'dataset.harmonized_table',
  schema: 'dataset.data_schema',
};

interface Props {
  /** the table to edit, a new one when undefined */
  table?: DatasetTable | undefined;
  mode?: Mode;
  /** the table is of a harmonization initiative (always in the schema mode) */
  harmonization?: boolean;
  /** the folder of the dataset, where the file sources are looked up first */
  folder?: string | undefined;
}

const props = withDefaults(defineProps<Props>(), { mode: 'collected', harmonization: false });
const show = defineModel<boolean>({ required: true });
/** the table, and whether it is of a harmonization initiative */
const emit = defineEmits<{ save: [table: DatasetTable, harmonization: boolean] }>();
const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const { t, locale } = useI18n();

const studies = ref<StudySummaryDto[]>([]);
const loadingStudies = ref(false);
const studyFilter = ref('');
const projects = ref<ProjectDto[]>([]);
const loadingProjects = ref(false);

/** the table is of a harmonization initiative */
const initiative = ref(false);
const studyId = ref<string>();
const populationId = ref<string>();
const dceId = ref<string>();
const source = ref<TableSource>({ namespace: 'opal' });
const showFiles = ref(false);
/** the folder of the file source when its path is absolute, the dataset folder otherwise */
const filesFolder = computed(() =>
  source.value.path?.startsWith('/') ? parentPath(source.value.path) : (props.folder ?? '/'),
);
/** the localized labels of the table, as `{field: {lang: value}}` */
const labels = ref<FormModel>({});

// the table labels form of the legacy admin app (`dataset-opal-table-schemaform.js`), with the additional information
const LOCALIZED = { type: 'object', format: 'localizedString' };
const LABEL_FIELDS = ['name', 'description', 'additionalInformation'] as const;
const labelsForm = computed(() =>
  toJsonForms(
    {
      type: 'object',
      properties: {
        name: { label: 't(name)', "options": { "dense": true }, ...LOCALIZED },
        description: { label: 't(description)', "options": { "dense": true }, ...LOCALIZED },
        additionalInformation: { label: 't(dataset.additional_information)', "options": { "dense": true }, ...LOCALIZED },
      },
    },
    [
      { key: 'name', type: 'localizedstring' },
      { key: 'description', type: 'localizedstring', rows: 3 },
      { key: 'additionalInformation', type: 'localizedstring', rows: 3 },
    ],
    { translate: (key: string) => t(key) },
  ),
);

function hasText(value: unknown) {
  return typeof value === 'object' && value !== null && Object.values(value).some((text) => `${text ?? ''}`.trim());
}

/** a label is optional, but once given it must be completed in all languages (the form checks the required ones) */
const labelsSchema = computed(() => ({
  ...labelsForm.value.schema,
  required: LABEL_FIELDS.filter((field) => hasText(labels.value[field])),
}));
// errors are shown after the first save attempt only, as in the documents forms
const validationMode = ref<ValidationMode>('ValidateAndHide');
const labelErrors = ref<ErrorObject[]>([]);

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
const required = computed(() => (props.mode === 'collected' ? ' *' : ''));
const complete = computed(
  () =>
    !!studyId.value &&
    (props.mode !== 'collected' || (!!populationId.value && !!dceId.value)) &&
    isSourceComplete(source.value),
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
    // the study states carry the population and event summaries
    const type = initiative.value ? 'harmonization-study' : 'individual-study';
    studies.value = (await documentsStore.fetchDocuments(documentTarget(type, ''))) as StudySummaryDto[];
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

function onKindChange(value: boolean) {
  initiative.value = value;
  studyId.value = undefined;
  onStudyChange();
  studies.value = [];
  loadStudies();
}

function onShow() {
  studyFilter.value = '';
  initiative.value = props.mode === 'schema' || props.harmonization;
  studies.value = [];
  studyId.value = props.table?.studyId || undefined;
  const table = props.table as DatasetDto_StudyTableDto | undefined;
  populationId.value = table?.populationId || undefined;
  dceId.value = table?.dataCollectionEventId || undefined;
  source.value = props.table ? tableSource(props.table) : { namespace: 'opal', project: '', table: '' };
  validationMode.value = 'ValidateAndHide';
  labels.value = Object.fromEntries(LABEL_FIELDS.map((field) => [field, localizedToObject(props.table?.[field])]));
  loadStudies();
}

/** the localized strings of a label, none when it has no text */
function labelValues(field: (typeof LABEL_FIELDS)[number]) {
  return hasText(labels.value[field]) ? (localizedToArray(labels.value[field]) ?? []) : [];
}

function onSave() {
  // no labels in the schema mode
  if (props.mode !== 'schema' && labelErrors.value.length > 0) {
    validationMode.value = 'ValidateAndShow';
    return;
  }
  // the study summary and the event unique id are computed by the server
  const table: DatasetDto_StudyTableDto = {
    ...(props.table as DatasetDto_StudyTableDto | undefined),
    studyId: studyId.value ?? '',
    populationId: populationId.value,
    dataCollectionEventId: dceId.value,
    name: labelValues('name'),
    description: labelValues('description'),
    additionalInformation: labelValues('additionalInformation'),
  };
  delete table.studySummary;
  delete table.dceId;
  if (initiative.value) {
    delete table.populationId;
    delete table.dataCollectionEventId;
  }
  emit('save', withSource(table, source.value), initiative.value);
  show.value = false;
}
</script>
