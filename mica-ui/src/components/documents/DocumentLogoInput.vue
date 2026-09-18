<template>
  <div>
    <div class="text-subtitle2 q-mb-sm">{{ t('logo') }}</div>
    <div class="row items-start q-gutter-md">
      <q-img v-if="modelValue" :src="downloadUrl(modelValue, documentPath)" style="max-width: 200px" fit="contain" />
      <div class="col">
        <q-file
          v-model="file"
          :label="modelValue ? modelValue.fileName : t('document.logo_hint')"
          accept="image/*"
          dense
          outlined
          clearable
          :loading="uploading"
          :disable="disable"
          @update:model-value="onFileSelected"
        >
          <template v-slot:prepend>
            <q-icon name="image" />
          </template>
        </q-file>
        <q-linear-progress v-if="uploading" :value="progress / 100" class="q-mt-xs" />
        <q-btn
          v-if="modelValue"
          flat
          dense
          size="sm"
          color="negative"
          icon="delete"
          :label="t('document.remove_logo')"
          class="q-mt-xs"
          :disable="disable || uploading"
          @click="onRemove"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AttachmentDto } from 'src/models/Mica';
import { useTempFiles } from 'src/composables/useTempFiles';
import { notifyError } from 'src/utils/notify';

interface Props {
  modelValue: AttachmentDto | undefined;
  /** REST path of the document, where a stored logo is downloaded from */
  documentPath: string;
  disable?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [logo: AttachmentDto | undefined] }>();
const { t } = useI18n();
const { upload, remove, downloadUrl } = useTempFiles();

const file = ref<File | null>(null);
const uploading = ref(false);
const progress = ref(0);

async function onFileSelected(selected: File | null) {
  if (!selected) return;
  uploading.value = true;
  progress.value = 0;
  try {
    const attachment = await upload(selected, (percent) => (progress.value = percent));
    emit('update:modelValue', attachment);
  } catch (error) {
    notifyError(error);
  } finally {
    uploading.value = false;
    file.value = null;
  }
}

async function onRemove() {
  const current = props.modelValue;
  emit('update:modelValue', undefined);
  if (current) {
    try {
      await remove(current);
    } catch (error) {
      console.warn('Temporary file deletion failed', error);
    }
  }
}
</script>
