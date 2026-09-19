<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-sm">
      <q-form @submit="onSave">
        <q-card-section>
          <div class="text-h6">{{ t('config.add_role') }}</div>
        </q-card-section>
        <q-separator />
        <q-card-section>
          <q-input
            v-model="role"
            dense
            outlined
            autofocus
            :label="t('config.role_id') + ' *'"
            :hint="t('config.role_help')"
            :rules="rules"
            lazy-rules
          />
        </q-card-section>
        <q-separator />
        <q-card-actions align="right" class="bg-grey-3">
          <q-btn flat :label="t('cancel')" color="secondary" :disable="saving" v-close-popup />
          <q-btn flat type="submit" :label="t('save')" color="primary" :loading="saving" />
        </q-card-actions>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { ValidationRule } from 'quasar';
import { hasRoleSpecialCharacters } from 'src/utils/config';

interface Props {
  modelValue: boolean;
  /** the existing roles: the identifier must be unique */
  roles: string[];
  saving?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; save: [role: string] }>();
const { t } = useI18n();

const showDialog = ref(props.modelValue);
const role = ref('');

const rules: ValidationRule[] = [
  (value: string) => !!value?.trim() || t('required'),
  (value: string) => !hasRoleSpecialCharacters(value.trim()) || t('config.role_character_error'),
  (value: string) => !props.roles.includes(value.trim()) || t('config.role_exists_error'),
];

function onSave() {
  emit('save', role.value.trim());
}

function onHide() {
  emit('update:modelValue', false);
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) role.value = '';
    showDialog.value = value;
  },
);
</script>
