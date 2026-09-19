<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-sm" style="min-width: 420px">
      <q-card-section>
        <div class="text-h6">{{ title }}</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-select
          v-model="form.type"
          :options="typeOptions"
          emit-value
          map-options
          dense
          outlined
          :label="t('type') + ' *'"
          :disable="editing"
          class="q-mb-md"
        />
        <q-input
          v-model="form.principal"
          dense
          outlined
          :label="t('permission.principal') + ' *'"
          :hint="principalHint"
          :disable="editing"
          :error="attempted && !form.principal.trim()"
          :error-message="t('required')"
          class="q-mb-md"
        />
        <div v-if="withRole" class="q-mb-md">
          <div class="text-caption">{{ t('role') }} *</div>
          <q-option-group v-model="form.role" :options="roleOptions" dense>
            <template v-slot:label="option">
              <div>{{ option.label }}</div>
              <div class="text-caption text-grey-7">{{ option.help }}</div>
            </template>
          </q-option-group>
        </div>
        <q-checkbox v-if="fileLabel" v-model="form.file" dense :label="fileLabel" />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn flat :label="t('save')" color="primary" :loading="saving" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { AclDto } from 'src/models/MicaSecurity';
import { DOCUMENT_ROLES, type AclInput, type AclType, type DocumentRole } from 'src/composables/useAcl';

interface Props {
  modelValue: boolean;
  /** the ACL edited; none to add one */
  acl?: AclDto | undefined;
  /** a permission has a role; an access has none */
  withRole?: boolean;
  title: string;
  principalHint: string;
  /** the label of the "apply to files" option; none to hide it */
  fileLabel?: string | undefined;
  saving?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; save: [acl: AclInput] }>();
const { t } = useI18n();

const showDialog = ref(props.modelValue);
const attempted = ref(false);
const form = ref<AclInput>(empty());

function empty(): AclInput {
  return { principal: '', type: 'USER', role: 'READER', file: true };
}

const editing = computed(() => props.acl !== undefined);

watch(
  () => props.modelValue,
  (value) => {
    showDialog.value = value;
    if (value) {
      attempted.value = false;
      form.value = props.acl
        ? {
            principal: props.acl.principal,
            type: props.acl.type as AclType,
            role: (props.acl.role as DocumentRole | undefined) ?? 'READER',
            file: props.acl.file !== false,
          }
        : empty();
    }
  },
);

const typeOptions = computed(() => [
  { label: t('permission.user'), value: 'USER' },
  { label: t('permission.group'), value: 'GROUP' },
]);

const roleOptions = computed(() =>
  DOCUMENT_ROLES.map((role) => ({
    label: t(`permission.${role.toLowerCase()}`),
    value: role,
    help: t(`permission.${role.toLowerCase()}_help`),
  })),
);

function onSave() {
  attempted.value = true;
  const principal = form.value.principal.trim();
  if (!principal) return;
  emit('save', {
    ...form.value,
    principal,
    role: props.withRole ? form.value.role : undefined,
    file: props.fileLabel ? form.value.file : undefined,
  });
}

function onHide() {
  emit('update:modelValue', false);
}
</script>
