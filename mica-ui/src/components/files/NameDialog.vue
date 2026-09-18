<template>
  <q-dialog v-model="showDialog" @hide="onHide">
    <q-card class="dialog-sm" style="min-width: 360px">
      <q-card-section>
        <div class="text-h6">{{ title }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-input
          v-model="name"
          dense
          outlined
          autofocus
          :label="label"
          :error="attempted && !valid"
          :error-message="t('files.invalid_name')"
          @keyup.enter="onSubmit"
        />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn flat :label="t('save')" color="primary" @click="onSubmit" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
interface Props {
  modelValue: boolean;
  title: string;
  label: string;
  /** initial value (rename) */
  initial?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submit: [name: string] }>();
const { t } = useI18n();

const showDialog = ref(props.modelValue);
const name = ref('');
const attempted = ref(false);

watch(
  () => props.modelValue,
  (value) => {
    showDialog.value = value;
    if (value) {
      name.value = props.initial ?? '';
      attempted.value = false;
    }
  },
);

/** a plain name: no separator, not empty, not a dot name */
const valid = computed(() => {
  const value = name.value.trim();
  return value.length > 0 && !value.includes('/') && !value.includes('\\') && value !== '.' && value !== '..';
});

function onSubmit() {
  attempted.value = true;
  if (!valid.value) return;
  emit('submit', name.value.trim());
  showDialog.value = false;
}

function onHide() {
  emit('update:modelValue', false);
}
</script>
