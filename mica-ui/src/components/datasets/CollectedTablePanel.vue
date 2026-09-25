<template>
  <div>
    <div class="text-hint q-mb-md">{{ t('dataset.study_table_info') }}</div>

    <div v-if="!table">
      <div class="q-mb-md">{{ t('dataset.no_study_table') }}</div>
      <q-btn
        v-if="canEdit"
        color="primary"
        icon="add"
        :label="t('dataset.add_study_table')"
        size="sm"
        :disable="busy"
        @click="showEdit = true"
      />
    </div>
    <template v-else>
      <div class="row items-center q-gutter-sm q-mb-md">
        <q-btn
          v-if="canEdit"
          color="primary"
          icon="edit"
          :label="t('edit')"
          size="sm"
          :disable="busy"
          @click="showEdit = true"
        />
        <q-btn
          v-if="canEdit"
          color="negative"
          icon="delete"
          :label="t('delete')"
          size="sm"
          :disable="busy"
          @click="showDelete = true"
        />
        <q-btn
          v-if="summary"
          color="secondary"
          :icon="summary.published ? 'refresh' : 'publish'"
          :label="t(summary.published ? 'dataset.index_study' : 'dataset.publish_study')"
          size="sm"
          :loading="studyBusy"
          :disable="busy"
          @click="onStudyAction"
        />
      </div>
      <fields-list :items="items" :dbobject="rows" />
    </template>

    <dataset-table-dialog v-model="showEdit" :table="table" @save="onSave" />
    <confirm-dialog
      v-model="showDelete"
      :title="t('dataset.delete_study_table')"
      :text="t('dataset.delete_study_table_text')"
      @confirm="onDelete"
    />
  </div>
</template>

<script setup lang="ts">
import type { DatasetDto, DatasetDto_StudyTableDto } from 'src/models/Mica';
import { api } from 'src/boot/api';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import FieldsList, { type FieldItem } from 'src/components/FieldsList.vue';
import DatasetTableDialog from 'src/components/datasets/DatasetTableDialog.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { useDocumentActions } from 'src/composables/useDocumentActions';
import { sourceFields, tableSource, type DatasetTable } from 'src/utils/datasets';
import { notifyError, notifySuccess } from 'src/utils/notify';
import { localized } from 'src/utils/persons';

interface Props {
  dataset: DatasetDto;
  canEdit: boolean;
  /** a change is being saved */
  busy?: boolean;
}

const props = defineProps<Props>();
/** the dataset with the table changed, to be saved; `refresh` when the study was published or indexed */
const emit = defineEmits<{ change: [dataset: DatasetDto]; refresh: [] }>();
const { t, locale } = useI18n();

const showEdit = ref(false);
const showDelete = ref(false);

const table = computed(() => props.dataset.collected?.studyTable);
const summary = computed(() => table.value?.studySummary);
const source = computed(() => tableSource(table.value));
const { busy: studyBusy, apply: applyStudy } = useDocumentActions(() =>
  documentTarget('individual-study', table.value?.studyId ?? ''),
);

function label(id: string | undefined, name: Parameters<typeof localized>[0]) {
  const text = localized(name, locale.value);
  return id && text ? `${id} - ${text}` : (id ?? '');
}

/** the table as displayed */
const rows = computed(() => {
  const value = table.value;
  if (!value) return {};
  const population = summary.value?.populationSummaries?.find((item) => item.id === value.populationId);
  const dce = population?.dataCollectionEventSummaries?.find((item) => item.id === value.dataCollectionEventId);
  return {
    study: label(value.studyId, summary.value?.acronym),
    population: label(value.populationId, population?.name),
    dce: label(value.dataCollectionEventId, dce?.name),
    ...source.value,
    namespace: t(`dataset.source.${source.value.namespace}.title`),
    // empty labels are shown as missing
    name: localized(value.name, locale.value) || undefined,
    description: localized(value.description, locale.value) || undefined,
    additionalInformation: localized(value.additionalInformation, locale.value) || undefined,
  };
});

const items = computed<FieldItem[]>(() => [
  {
    field: 'study',
    label: 'dataset.study',
    links: (row) => [{ label: row.study, to: `/individual-study/${table.value?.studyId}` }],
  },
  { field: 'population', label: 'study.population' },
  { field: 'dce', label: 'dataset.dce' },
  { field: 'namespace', label: 'dataset.source.title' },
  ...sourceFields(source.value.namespace),
  { field: 'name', label: 'name' },
  { field: 'description', label: 'description' },
  { field: 'additionalInformation', label: 'dataset.additional_information' },
]);

function onSave(studyTable: DatasetTable) {
  emit('change', { ...props.dataset, collected: { studyTable: studyTable as DatasetDto_StudyTableDto } });
}

function onDelete() {
  const dataset = { ...props.dataset };
  delete dataset.collected;
  emit('change', dataset);
}

/** publishes the study of the table, or indexes it again when published (e.g. after the dataset publication) */
async function onStudyAction() {
  const id = table.value?.studyId ?? '';
  if (summary.value?.published) {
    try {
      await api.put('/draft/individual-studies/_index', null, { params: { id } });
      notifySuccess(t('dataset.study_indexed', { id }));
    } catch (error) {
      notifyError(error);
    }
  } else if ((await applyStudy({ type: 'publish' })) === 'updated') {
    emit('refresh');
  }
}
</script>
