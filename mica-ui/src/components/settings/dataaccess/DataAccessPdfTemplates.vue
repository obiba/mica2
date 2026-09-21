<template>
  <div v-if="dto" class="q-mt-lg">
    <div class="text-h6">{{ t('config.data_access.pdf_download') }}</div>
    <p class="text-grey-8">{{ t('config.data_access.pdf_download_info') }}</p>
    <q-option-group :model-value="dto.pdfDownloadType" :options="downloadTypes" @update:model-value="setDownloadType" />
    <div v-if="dto.pdfDownloadType === DataAccessFormDto_PdfDownloadType.Template" class="q-ml-lg q-mt-sm">
      <p class="text-grey-8">{{ t('config.data_access.pdf_template_info') }}</p>
      <div v-for="language in systemStore.languages" :key="language" class="row items-center q-col-gutter-md q-mb-sm">
        <div class="col-12 col-md-4">
          <q-file
            :model-value="null"
            :label="t('config.data_access.pdf_template_file', { language: languageLabel(language) })"
            accept=".pdf,application/pdf"
            dense
            outlined
            :loading="uploading === language"
            @update:model-value="(file: File | null) => onFileSelected(language, file)"
          >
            <template #prepend>
              <q-icon name="picture_as_pdf" />
            </template>
          </q-file>
        </div>
        <div class="col-12 col-md-8">
          <template v-if="templateOf(language)">
            <q-chip
              removable
              icon="description"
              :label="`${templateOf(language)?.fileName} (${sizeLabel(templateOf(language)?.size)})`"
              :title="t('config.data_access.pdf_template_remove')"
              @remove="onRemove(language)"
            />
            <q-badge v-if="templateOf(language)?.justUploaded" color="orange" class="q-ml-xs">{{
              t('config.data_access.pdf_template_new')
            }}</q-badge>
          </template>
          <span v-else class="text-grey-7">{{ t('config.data_access.pdf_template_none') }}</span>
        </div>
      </div>
    </div>
    <p v-else class="text-grey-8 q-ml-lg q-mt-sm">{{ t('config.data_access.pdf_embedded_info') }}</p>
  </div>
</template>

<script setup lang="ts">
import type { ConfigFormState } from 'src/composables/useConfigForm';
import { useTempFiles } from 'src/composables/useTempFiles';
import { DataAccessFormDto_PdfDownloadType, type AttachmentDto, type DataAccessFormDto } from 'src/models/Mica';
import { sizeLabel } from 'src/utils/files';
import { notifyError } from 'src/utils/notify';

interface Props {
  /** the application form, whose DTO holds the PDF download type and the templates */
  state: ConfigFormState<DataAccessFormDto>;
}

const props = defineProps<Props>();
const { t, locale } = useI18n();
const systemStore = useSystemStore();
const { upload, remove } = useTempFiles();

const state = reactive(props.state);
const dto = computed(() => state.dto);
const uploading = ref<string>();

const downloadTypes = computed(() => [
  { value: DataAccessFormDto_PdfDownloadType.Template, label: t('config.data_access.pdf_template') },
  { value: DataAccessFormDto_PdfDownloadType.Embedded, label: t('config.data_access.pdf_embedded') },
]);

const languageNames = computed(() => systemStore.availableLanguages[locale.value] || {});

function languageLabel(code: string): string {
  const name = languageNames.value[code];
  return name ? `${name} (${code})` : code;
}

/** the templates, one per language (the server omits the empty list) */
const templates = computed<AttachmentDto[]>(() => dto.value?.pdfTemplates ?? []);

/** the template of a language */
function templateOf(language: string): AttachmentDto | undefined {
  return templates.value.find((template) => template.lang === language);
}

function setDownloadType(type: DataAccessFormDto_PdfDownloadType) {
  if (!dto.value) return;
  dto.value.pdfDownloadType = type;
  state.dirty = true;
}

async function onFileSelected(language: string, file: File | null) {
  if (!file || !dto.value) return;
  uploading.value = language;
  try {
    const attachment = await upload(file);
    attachment.lang = language;
    const previous = templateOf(language);
    dto.value.pdfTemplates = [...templates.value.filter((template) => template.lang !== language), attachment];
    state.dirty = true;
    if (previous?.justUploaded) await remove(previous).catch(() => undefined);
  } catch (error) {
    notifyError(error);
  } finally {
    uploading.value = undefined;
  }
}

async function onRemove(language: string) {
  if (!dto.value) return;
  const previous = templateOf(language);
  dto.value.pdfTemplates = templates.value.filter((template) => template.lang !== language);
  state.dirty = true;
  if (previous?.justUploaded) await remove(previous).catch(() => undefined);
}

onMounted(() => {
  systemStore.loadLanguages(locale.value).catch(() => undefined);
});
</script>
