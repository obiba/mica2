<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('system.translations.add') }}</div>
      </q-card-section>

      <q-separator />

      <q-card-section>
        <q-form ref="formRef">
          <q-input
            v-model="newName"
            dense
            type="text"
            :label="t('name') + ' *'"
            :hint="t('system.translations.name_hint')"
            class="q-mb-md"
            lazy-rules
            :rules="[validateRequired, validateUnique]"
          >
          </q-input>
          <div class="text-help">{{ t('translations') }}</div>
          <template v-for="language in languages" :key="language">
            <q-input v-model="newValues[language]" dense type="text" class="q-mb-md" lazy-rules>
              <template v-slot:prepend>
                <q-chip :label="language.toUpperCase()" size="sm" />
              </template>
            </q-input>
          </template>
        </q-form>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" @click="onCancel" v-close-popup />
        <q-btn flat :label="t('add')" color="primary" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
interface DialogProps {
  modelValue: boolean;
  translationKeys: string[];
  languages: string[];
}

const { t } = useI18n();
const props = defineProps<DialogProps>();
const emit = defineEmits(['update:modelValue', 'added', 'cancel']);
const formRef = ref();
const showDialog = ref(props.modelValue);
const newName = ref('');
const newValues = ref<{ [key: string]: string }>({});

function validateRequired(value: string) {
  return !!value || t('name_required');
}

function validateUnique(value: string) {
  if (value) {
    const exists = props.translationKeys.find((key) => key === value);
    return !exists || t('system.translations.name_exists');
  }
  return true;
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      newName.value = '';
      props.languages.forEach((language: string) => {
        newValues.value[language] = '';
      });
    }

    showDialog.value = value;
  },
);

function onHide() {
  emit('update:modelValue', false);
}

function onCancel() {
  emit('cancel');
}

async function onSave() {
  const valid = await formRef.value.validate();
  if (valid) {
    Object.keys(newValues.value).forEach((key) => {
      if (!newValues.value[key]) {
        newValues.value[key] = newName.value;
      }
    });

    emit('added', newName.value, newValues.value);
    emit('update:modelValue', false);
  }
}
</script>
