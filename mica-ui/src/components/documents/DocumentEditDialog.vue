<template>
  <q-dialog v-model="show" persistent @before-show="initialize">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ id ? `${t('edit')} [${id}]` : t(target.labels.new) }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll">
        <q-spinner-dots v-if="loading" color="primary" size="2em" />
        <div v-else-if="document">
          <document-logo-input
            v-if="target.withLogo"
            v-model="logo"
            :document-path="target.path"
            :disable="saving"
            class="q-mb-md"
          />
          <div v-if="withSchema" class="row items-center q-gutter-sm q-mb-md">
            <span class="text-subtitle1">{{ t('dataset.data_schema') }} *</span>
            <span v-if="schema">{{ schema.studyId }} - {{ sourceText(tableSource(schema)) }}</span>
            <span v-else class="text-grey-7">{{ t('dataset.no_data_schema') }}</span>
            <q-btn
              color="primary"
              :icon="schema ? 'edit' : 'add'"
              :label="t(schema ? 'edit' : 'dataset.set_data_schema')"
              size="sm"
              :disable="saving"
              @click="showSchema = true"
            />
            <dataset-table-dialog
              v-model="showSchema"
              mode="schema"
              :table="schema"
              @save="(table) => (schema = table)"
            />
          </div>
          <entity-json-form ref="form" v-model="model" :form-path="target.formPath" />
          <div v-if="invalid" class="text-negative q-mt-sm">{{ t('missing_required_fields') }}</div>
        </div>
        <div v-else>
          {{ t('document.not_found') }}
        </div>
      </q-card-section>
      <q-separator />
      <document-save-bar
        :with-comment="!!id"
        :comment-required="commentRequired"
        :saving="saving"
        @save="onSave"
        @cancel="onCancel"
      />
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { AttachmentDto, DatasetDto } from 'src/models/Mica';
import DatasetTableDialog from 'src/components/datasets/DatasetTableDialog.vue';
import DocumentLogoInput from 'src/components/documents/DocumentLogoInput.vue';
import DocumentSaveBar from 'src/components/documents/DocumentSaveBar.vue';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import { useDocumentTarget, type DocumentType } from 'src/composables/useDocumentTarget';
import { modelSnapshot, useDocumentModel, type FormModel } from 'src/composables/useDocumentModel';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import type { DocumentDto } from 'src/stores/documents';
import { notifyError } from 'src/utils/notify';
import { sourceText, tableSource, type DatasetTable } from 'src/utils/datasets';

interface Props {
  type: DocumentType;
  /** the document to edit, a new one when undefined */
  id?: string | undefined;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ saved: [id: string] }>();
const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const { t } = useI18n();

const { target } = useDocumentTarget(
  () => props.type,
  () => props.id ?? '',
);
const { toModel, fromModel } = useDocumentModel(() => props.type);

const form = ref<InstanceType<typeof EntityJsonForm>>();
const loading = ref(true);
const saving = ref(false);
const invalid = ref(false);
const document = ref<DocumentDto>();
const model = ref<FormModel>({});
const logo = ref<AttachmentDto>();
/** a harmonized dataset cannot be created without its data schema, edited afterwards in the tables tab */
const withSchema = computed(() => !props.id && props.type === 'harmonized-dataset');
const schema = ref<DatasetTable>();
const showSchema = ref(false);
/** snapshot of what was loaded, for the dirty check */
const snapshot = ref('');

const commentRequired = computed(() => systemStore.configuration.isCommentsRequiredOnDocumentSave === true);
const dirty = computed(() => show.value && !loading.value && snapshotOf() !== snapshot.value);
const { confirmLeave } = useConfirmLeave(dirty);

function snapshotOf() {
  return modelSnapshot({ model: model.value, logo: logo.value?.id, schema: schema.value });
}

async function initialize() {
  loading.value = true;
  invalid.value = false;
  schema.value = undefined;
  try {
    const loaded = props.id
      ? (await documentsStore.fetchDocument(target.value)).document
      : documentsStore.newDocument(props.type);
    document.value = loaded;
    model.value = toModel(loaded);
    logo.value = 'logo' in loaded ? loaded.logo : undefined;
    snapshot.value = snapshotOf();
  } catch (error) {
    document.value = undefined;
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

async function onSave(comment: string | undefined) {
  const current = document.value;
  if (!current) return;
  invalid.value = !form.value?.validate() || (withSchema.value && !schema.value);
  if (invalid.value) return;
  const dto = fromModel(current, model.value);
  if (withSchema.value && schema.value) {
    (dto as DatasetDto).protocol = { harmonizationTable: schema.value, studyTables: [], harmonizationTables: [] };
  }
  if (target.value.withLogo) {
    const withLogo = dto as { logo?: AttachmentDto | undefined };
    if (logo.value) {
      withLogo.logo = logo.value;
    } else {
      delete withLogo.logo;
    }
  }
  saving.value = true;
  try {
    const documentId = props.id
      ? (await documentsStore.saveDocument(target.value, dto, comment), props.id)
      : await documentsStore.createDocument(target.value, dto);
    show.value = false;
    emit('saved', documentId);
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}

async function onCancel() {
  if (await confirmLeave()) show.value = false;
}
</script>
