<template>
  <q-dialog v-model="showDialog" @show="onShow" @hide="onHide">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('config.plugins.configure_title', { name }) }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-input
          v-model="properties"
          :label="t('config.plugins.site_properties')"
          :hint="t('config.plugins.site_properties_hint')"
          :loading="loading"
          type="textarea"
          input-style="font-family: monospace"
          autogrow
          outlined
          dense
        />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn :label="t('save')" color="primary" :loading="saving" :disable="loading" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { notifyError } from 'src/utils/notify';

const props = defineProps<{
  modelValue: boolean;
  name: string;
  load: (name: string) => Promise<string>;
  save: (name: string, properties: string) => Promise<boolean>;
}>();
const emit = defineEmits(['update:modelValue', 'saved']);

const { t } = useI18n();

const showDialog = ref(props.modelValue);
const properties = ref('');
const loading = ref(false);
const saving = ref(false);

watch(
  () => props.modelValue,
  (value) => (showDialog.value = value),
);

async function onShow() {
  properties.value = '';
  loading.value = true;
  try {
    properties.value = await props.load(props.name);
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

function onHide() {
  emit('update:modelValue', false);
}

async function onSave() {
  saving.value = true;
  const saved = await props.save(props.name, properties.value);
  saving.value = false;
  if (saved) {
    emit('saved');
    showDialog.value = false;
  }
}
</script>
