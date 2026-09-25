<template>
  <q-dialog v-model="show" persistent @before-show="initialize">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ t(`studies_import.title.${type}`) }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll">
        <q-banner v-if="error" dense rounded class="bg-negative text-white q-mb-md">{{ t(error) }}</q-banner>
        <q-stepper v-model="step" flat animated active-color="primary" done-color="positive">
          <q-step name="connection" :title="t('studies_import.connection')" icon="link" :done="stepIndex > 0">
            <div class="text-hint q-mb-md">{{ t('studies_import.connection_hint') }}</div>
            <q-form ref="connectionForm" class="q-gutter-sm">
              <q-input
                v-model="connection.url"
                type="url"
                :label="t('studies_import.url') + ' *'"
                :hint="t('studies_import.url_hint')"
                :rules="[(val) => /^https?:\/\/.+/.test(val) || t('studies_import.url_hint')]"
                dense
              />
              <q-input
                v-model="connection.username"
                :label="t('username') + ' *'"
                :rules="[(val) => !!val || t('required')]"
                autocomplete="off"
                dense
              />
              <q-input
                v-model="connection.password"
                type="password"
                :label="t('studies_import.password') + ' *'"
                :rules="[(val) => !!val || t('required')]"
                autocomplete="new-password"
                dense
              />
            </q-form>
          </q-step>

          <q-step name="differences" :title="t('studies_import.differences')" icon="compare" :done="stepIndex > 1">
            <div class="text-hint q-mb-md">{{ connection.url }}</div>
            <q-list bordered separator dense>
              <q-item v-for="diff in diffs" :key="diff.section">
                <q-item-section :style="{ paddingLeft: `${diff.depth * 24}px` }">
                  {{ t(`studies_import.sections.${diff.section}`) }}
                </q-item-section>
                <q-item-section side>
                  <div class="row items-center no-wrap q-gutter-xs">
                    <q-icon :name="diffIcon(diff)" :color="diffColor(diff)" size="sm" />
                    <span>{{ t(diffLabel(diff)) }}</span>
                  </div>
                </q-item-section>
              </q-item>
            </q-list>
            <div class="text-hint q-mt-md">
              {{ t('studies_import.differences_hint', { section: diffs[0] ? sectionLabel(diffs[0].section) : '' }) }}
            </div>
          </q-step>

          <q-step name="studies" :title="t('studies_import.studies')" icon="checklist" :done="stepIndex > 2">
            <q-table
              v-model:selected="selected"
              flat
              dense
              :rows="remoteStudies"
              :columns="columns"
              row-key="id"
              selection="multiple"
              :filter="filter"
              :pagination="{ rowsPerPage: 10 }"
              :rows-per-page-options="[10, 25, 50]"
            >
              <template v-slot:top-right>
                <q-input v-model="filter" dense debounce="300" :placeholder="t('search')">
                  <template v-slot:append>
                    <q-icon name="search" />
                  </template>
                </q-input>
              </template>
              <template v-slot:body-cell-status="props">
                <q-td :props="props">
                  <document-status-badge :state="props.row.state" show-status />
                </q-td>
              </template>
            </q-table>
          </q-step>

          <q-step name="summary" :title="t('studies_import.summary')" icon="fact_check" :done="stepIndex > 3">
            <div v-if="operations.toCreate.length">
              <div class="text-subtitle2">{{ t('studies_import.to_create') }}</div>
              <q-list dense separator class="q-mb-md">
                <q-item v-for="study in operations.toCreate" :key="study.id">
                  <q-item-section>{{ studyLabel(study) }}</q-item-section>
                  <q-item-section side>
                    <q-btn flat round dense size="sm" icon="close" :title="t('delete')" @click="remove(study)" />
                  </q-item-section>
                </q-item>
              </q-list>
            </div>
            <div v-if="operations.toReplace.length">
              <div class="text-subtitle2">{{ t('studies_import.to_replace') }}</div>
              <div class="text-hint">{{ t('studies_import.confirmation_hint') }}</div>
              <q-list dense separator class="q-mb-md">
                <q-item v-for="study in operations.toReplace" :key="study.id">
                  <q-item-section>{{ studyLabel(study) }}</q-item-section>
                  <q-item-section side>
                    <div class="row items-center no-wrap q-gutter-xs">
                      <q-badge color="info">
                        S<q-tooltip>{{ t('studies_import.replaced_study') }}</q-tooltip>
                      </q-badge>
                      <q-badge
                        v-if="study.localPopulationSize > 0 && isImportable(diffs, 'study-population')"
                        color="info"
                      >
                        P<q-tooltip>{{
                          t('studies_import.replaced_populations', { count: study.localPopulationSize })
                        }}</q-tooltip>
                      </q-badge>
                      <q-badge
                        v-if="study.localDCEsSize > 0 && isImportable(diffs, 'data-collection-event')"
                        color="info"
                      >
                        D<q-tooltip>{{ t('studies_import.replaced_dces', { count: study.localDCEsSize }) }}</q-tooltip>
                      </q-badge>
                      <q-input
                        v-model="confirmations[study.id]"
                        dense
                        :placeholder="study.id"
                        :error="!!confirmations[study.id] && confirmations[study.id] !== study.id"
                        hide-bottom-space
                        style="width: 160px"
                        class="q-ml-sm"
                      />
                      <q-btn flat round dense size="sm" icon="close" :title="t('delete')" @click="remove(study)" />
                    </div>
                  </q-item-section>
                </q-item>
              </q-list>
            </div>
            <div v-if="operations.conflicts.length">
              <div class="text-subtitle2">{{ t('studies_import.conflicts') }}</div>
              <q-list dense separator>
                <q-item v-for="study in operations.conflicts" :key="study.id">
                  <q-item-section>{{ studyLabel(study) }}</q-item-section>
                  <q-item-section side class="text-negative">{{ t(`studies_import.conflict.${type}`) }}</q-item-section>
                </q-item>
              </q-list>
            </div>
          </q-step>

          <q-step name="results" :title="t('studies_import.results')" icon="done_all">
            <q-list dense separator>
              <q-item v-for="result in results" :key="result.study.id">
                <q-item-section>{{ studyLabel(result.study) }}</q-item-section>
                <q-item-section side>{{ t(`studies_import.${result.operation}`) }}</q-item-section>
                <q-item-section side :class="result.status === 200 ? 'text-positive' : 'text-negative'">
                  {{ t(importStatusMessage(result.status)) }}
                </q-item-section>
              </q-item>
            </q-list>
          </q-step>
        </q-stepper>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('close')" color="secondary" @click="onClose" />
        <q-btn
          v-if="stepIndex > 0 && step !== 'results'"
          flat
          :label="t('studies_import.previous')"
          color="primary"
          :disable="busy"
          @click="onPrevious"
        />
        <q-btn
          v-if="step !== 'summary' && step !== 'results'"
          :label="t('studies_import.next')"
          color="primary"
          :loading="busy"
          :disable="!canNext"
          @click="onNext"
        />
        <q-btn
          v-if="step === 'summary'"
          :label="t('studies_import.import')"
          color="primary"
          :loading="busy"
          :disable="!canFinish"
          @click="onFinish"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { QForm, QTableColumn } from 'quasar';
import type { StudySummaryDto } from 'src/models/Mica';
import DocumentStatusBadge from 'src/components/documents/DocumentStatusBadge.vue';
import {
  canImport,
  differentSections,
  fetchDifferences,
  fetchImportOperations,
  fetchRemoteStudies,
  importStatusMessage,
  isImportable,
  saveStudies,
  type FormSectionDiff,
  type ImportOperations,
  type ImportStudyType,
} from 'src/composables/useStudiesImport';
import { notifyError } from 'src/utils/notify';
import { localized } from 'src/utils/persons';

interface Props {
  type: ImportStudyType;
}

interface ImportResult {
  study: StudySummaryDto;
  operation: 'create' | 'replace';
  status: number;
}

const STEPS = ['connection', 'differences', 'studies', 'summary', 'results'] as const;
type Step = (typeof STEPS)[number];

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ imported: [] }>();
const { t, locale } = useI18n();

const step = ref<Step>('connection');
const stepIndex = computed(() => STEPS.indexOf(step.value));
const busy = ref(false);
/** the i18n key of the last remote problem */
const error = ref<string>();
const connectionForm = ref<QForm>();
const connection = ref({ url: '', username: '', password: '' });
const diffs = ref<FormSectionDiff[]>([]);
const remoteStudies = ref<StudySummaryDto[]>([]);
const selected = ref<StudySummaryDto[]>([]);
const filter = ref('');
const operations = ref<ImportOperations>({ toCreate: [], toReplace: [], conflicts: [] });
/** the ids typed to confirm the replacements */
const confirmations = ref<Record<string, string>>({});
const results = ref<ImportResult[]>([]);

const columns = computed<QTableColumn<StudySummaryDto>[]>(() => [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  {
    name: 'acronym',
    label: t('acronym'),
    field: (row) => localized(row.acronym, locale.value),
    align: 'left',
    sortable: true,
  },
  {
    name: 'name',
    label: t('name'),
    field: (row) => localized(row.name, locale.value),
    align: 'left',
    sortable: true,
    style: 'white-space: normal',
  },
  { name: 'status', label: t('status'), field: 'state', align: 'left' },
]);

const canNext = computed(() => {
  if (step.value === 'differences') return canImport(diffs.value);
  if (step.value === 'studies') return selected.value.length > 0;
  return step.value === 'connection';
});

const canFinish = computed(
  () =>
    operations.value.toCreate.length + operations.value.toReplace.length > 0 &&
    operations.value.toReplace.every((study) => confirmations.value[study.id] === study.id),
);

function initialize() {
  step.value = 'connection';
  error.value = undefined;
  connection.value = { url: '', username: '', password: '' };
  diffs.value = [];
  remoteStudies.value = [];
  selected.value = [];
  filter.value = '';
  results.value = [];
}

function sectionLabel(section: string) {
  return t(`studies_import.sections.${section}`);
}

function studyLabel(study: StudySummaryDto) {
  return `${localized(study.acronym, locale.value)} - ${localized(study.name, locale.value)} [${study.id}]`;
}

function diffIcon(diff: FormSectionDiff) {
  return diff.equal ? 'check_circle' : 'warning';
}

function diffColor(diff: FormSectionDiff) {
  return !diff.equal ? 'negative' : diff.importable ? 'positive' : 'warning';
}

function diffLabel(diff: FormSectionDiff) {
  return !diff.equal
    ? 'studies_import.section_different'
    : diff.importable
      ? 'studies_import.section_equal'
      : 'studies_import.section_parent_different';
}

/** runs a step request, a remote problem is shown in the dialog, any other error notified */
async function run(action: () => Promise<void>) {
  busy.value = true;
  error.value = undefined;
  try {
    await action();
  } catch (e) {
    if (typeof e === 'string') error.value = e;
    else notifyError(e);
  } finally {
    busy.value = false;
  }
}

async function onNext() {
  if (step.value === 'connection') {
    if (!(await connectionForm.value?.validate())) return;
    await run(async () => {
      diffs.value = await fetchDifferences(connection.value, props.type);
      step.value = 'differences';
    });
  } else if (step.value === 'differences') {
    await run(async () => {
      remoteStudies.value = await fetchRemoteStudies(connection.value, props.type);
      selected.value = [];
      step.value = 'studies';
    });
  } else if (step.value === 'studies') {
    await run(async () => {
      operations.value = await fetchImportOperations(selected.value, props.type);
      confirmations.value = {};
      step.value = 'summary';
    });
  }
}

function onPrevious() {
  error.value = undefined;
  step.value = STEPS[stepIndex.value - 1] ?? 'connection';
}

/** removes the study from the operations, back to the selection when there is nothing left to import */
function remove(study: StudySummaryDto) {
  const { toCreate, toReplace } = operations.value;
  operations.value.toCreate = toCreate.filter((s) => s.id !== study.id);
  operations.value.toReplace = toReplace.filter((s) => s.id !== study.id);
  selected.value = selected.value.filter((s) => s.id !== study.id);
  if (operations.value.toCreate.length + operations.value.toReplace.length === 0) step.value = 'studies';
}

async function onFinish() {
  const { toCreate, toReplace } = operations.value;
  await run(async () => {
    const statuses = await saveStudies(
      connection.value,
      props.type,
      [...toCreate, ...toReplace].map((study) => study.id),
      differentSections(diffs.value),
    );
    results.value = [
      ...toCreate.map((study) => ({ study, operation: 'create' as const, status: statuses[study.id] ?? 500 })),
      ...toReplace.map((study) => ({ study, operation: 'replace' as const, status: statuses[study.id] ?? 500 })),
    ];
    step.value = 'results';
  });
}

function onClose() {
  show.value = false;
  if (results.value.some((result) => result.status === 200)) emit('imported');
}
</script>
