<template>
  <q-dialog v-model="showDialog" @hide="onHide">
    <q-card class="dialog-sm" style="min-width: 480px">
      <q-card-section>
        <div class="text-h6">{{ t('files.edit_details') }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-input v-model="type" dense outlined :label="t('files.type')" :hint="t('files.type_hint')" class="q-mb-md" />
        <div class="text-caption q-mb-xs">{{ t('files.description') }}</div>
        <q-tabs v-model="lang" dense align="left" class="text-grey-7" active-color="primary">
          <q-tab v-for="language in languages" :key="language" :name="language" :label="language.toUpperCase()" />
        </q-tabs>
        <q-tab-panels v-model="lang">
          <q-tab-panel v-for="language in languages" :key="language" :name="language" class="q-px-none">
            <q-input v-model="description[language]" dense outlined type="textarea" autogrow />
          </q-tab-panel>
        </q-tab-panels>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn :label="t('save')" color="primary" :loading="saving" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { AttachmentDto } from 'src/models/Mica';
import { localizedToArray, localizedToObject } from 'src/composables/useDocumentModel';

export type FileDetails = Pick<AttachmentDto, 'type' | 'description'>;

interface Props {
  modelValue: boolean;
  /** the attachment edited */
  attachment: AttachmentDto;
  saving?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; save: [details: FileDetails] }>();
const { t } = useI18n();
const systemStore = useSystemStore();

const languages = computed(() => systemStore.languages);
const showDialog = ref(props.modelValue);
const type = ref('');
const description = ref<Record<string, string>>({});
const lang = ref(languages.value[0] ?? 'en');

watch(
  () => props.modelValue,
  (value) => {
    showDialog.value = value;
    if (value) {
      type.value = props.attachment.type ?? '';
      description.value = localizedToObject(props.attachment.description);
      lang.value = languages.value[0] ?? 'en';
    }
  },
);

function onSave() {
  const values = Object.fromEntries(Object.entries(description.value).filter(([, value]) => value?.trim()));
  emit('save', { type: type.value.trim() || undefined, description: localizedToArray(values) ?? [] });
}

function onHide() {
  emit('update:modelValue', false);
}
</script>
