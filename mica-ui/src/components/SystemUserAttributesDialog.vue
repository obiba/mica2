<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ editMode ? t('system.attributes.update') : t('system.attributes.add') }}</div>
      </q-card-section>

      <q-separator />
      <q-card-section>
        <q-form ref="formRef">
          <q-input
            v-model="newAttribue.name"
            dense
            type="text"
            :label="t('name') + ' *'"
            class="q-mb-md"
            lazy-rules
            :rules="[validateRequired, validateUnique]"
            :disable="editMode"
          >
          </q-input>
          <q-input
            v-model="newAttribue.description"
            :label="t('description')"
            class="q-mb-md"
            dense
            autogrow
            type="textarea"
            lazy-rules
          />
          <q-select
            v-model="type"
            :label="t('type') + ' *'"
            :options="typeOptions"
            class="q-mb-md"
            dense
            emit-value
            map-options
          />
          <q-input
            v-model="values"
            dense
            type="text"
            :label="t('values')"
            :hint="t('system.attributes.values_hint')"
            class="q-mb-md"
            v-show="newAttribue.type === 'STRING'"
          >
          </q-input>
          <q-checkbox v-model="newAttribue.required" :label="t('required')" class="q-mb-md" dense />
        </q-form>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" @click="onCancel" v-close-popup />
        <q-btn flat :label="t('save')" color="primary" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { AttributeConfigurationDto } from 'src/models/Mica';
import { notifyError, notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const systemStore = useSystemStore();

interface DialogProps {
  modelValue: boolean;
  attribute?: AttributeConfigurationDto;
}

const props = defineProps<DialogProps>();
const emit = defineEmits(['update:modelValue', 'saved', 'cancel']);
const formRef = ref();
const showDialog = ref(props.modelValue);
const newAttribue = ref<AttributeConfigurationDto>({} as AttributeConfigurationDto);
const type = computed({
  get: () => newAttribue.value.type,
  set: (value) => {
    newAttribue.value.type = value;
    if (value !== 'STRING') {
      newAttribue.value.values = [];
    }
  },
});
const values = computed({
  get: () => newAttribue.value.values?.join(', '),
  set: (value) => {
    newAttribue.value.values = value.split(/\s*,\s*/);
  },
});
const editMode = computed(() => !!props.attribute && !!props.attribute.name);
const typeOptions = computed(() =>
  ['STRING', 'NUMBER', 'BOOLEAN', 'INTEGER'].map((value) => ({ label: t(`system.attributes.types.${value}`), value })),
);

function validateRequired(value: string) {
  return !!value || t('name_required');
}

function validateUnique(value: string) {
  if (systemStore.userAttributes) {
    const exists = systemStore.userAttributes.find((attr) => attr.name === value);
    return !exists || t('system.attributes.name_exists');
  }
  return true;
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      if (props.attribute) {
        newAttribue.value = { ...props.attribute } as AttributeConfigurationDto;
      } else {
        newAttribue.value = { type: 'STRING', required: false } as AttributeConfigurationDto;
      }
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
    const endpoint = editMode.value ? systemStore.updateAttribute : systemStore.addAttribute;
    endpoint(newAttribue.value)
      .then(() => {
        notifySuccess(t('system.attributes.updated'));
        emit('saved');
      })
      .catch(() => {
        notifyError(t('system.attributes.update_failed'));
      })
      .finally(() => {
        newAttribue.value = { type: 'STRING' } as AttributeConfigurationDto;
        emit('update:modelValue', false);
      });
  }
}
</script>
