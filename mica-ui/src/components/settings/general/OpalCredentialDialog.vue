<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-sm">
      <q-form @submit="onSave">
        <q-card-section>
          <div class="text-h6">{{ t('config.opal_credential') }}</div>
        </q-card-section>
        <q-separator />
        <q-card-section>
          <q-input
            v-model="form.opalUrl"
            dense
            outlined
            type="url"
            :label="t('config.opal_url') + ' *'"
            :disable="editing"
            :rules="[required]"
            lazy-rules
            class="q-mb-md"
          />
          <template v-if="kind === 'USERNAME'">
            <q-input
              v-model="form.username"
              dense
              outlined
              autocomplete="off"
              :label="t('config.username') + ' *'"
              :rules="[required]"
              lazy-rules
              class="q-mb-md"
            />
            <q-input
              v-model="form.password"
              dense
              outlined
              type="password"
              autocomplete="new-password"
              :label="t('auth.password') + ' *'"
              :rules="[required]"
              lazy-rules
              class="q-mb-md"
            />
            <q-input
              v-model="confirm"
              dense
              outlined
              type="password"
              autocomplete="new-password"
              :label="t('config.confirm_password') + ' *'"
              :rules="[required, (value: string) => value === form.password || t('config.password_mismatch')]"
              lazy-rules
              class="q-mb-md"
            />
          </template>
          <q-input
            v-else
            v-model="form.token"
            dense
            outlined
            autocomplete="off"
            :label="t('config.opal_token') + ' *'"
            :rules="[required]"
            lazy-rules
            class="q-mb-md"
          />
        </q-card-section>
        <q-separator />
        <q-card-actions align="right" class="bg-grey-3">
          <q-btn flat :label="t('cancel')" color="secondary" :disable="saving" v-close-popup />
          <q-btn type="submit" :label="t('save')" color="primary" :loading="saving" />
        </q-card-actions>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { OpalCredentialType, type OpalCredentialDto } from 'src/models/Mica';

interface Props {
  modelValue: boolean;
  /** a user name and password, or a personal access token */
  kind: 'USERNAME' | 'TOKEN';
  /** the credential edited (its Opal URL cannot change); none to add one */
  credential?: OpalCredentialDto | undefined;
  saving?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; save: [credential: OpalCredentialDto] }>();
const { t } = useI18n();

const showDialog = ref(props.modelValue);
const form = ref<OpalCredentialDto>(empty());
const confirm = ref('');

const editing = computed(() => props.credential !== undefined);
const required = (value: string) => !!value?.trim() || t('required');

function empty(): OpalCredentialDto {
  return {
    type: props.kind === 'USERNAME' ? OpalCredentialType.USERNAME : OpalCredentialType.TOKEN,
    opalUrl: '',
    username: '',
    password: '',
    token: '',
  };
}

function onSave() {
  const credential: OpalCredentialDto = { type: form.value.type, opalUrl: form.value.opalUrl.trim() };
  if (props.kind === 'USERNAME') {
    credential.username = form.value.username;
    credential.password = form.value.password;
  } else {
    credential.token = form.value.token;
  }
  emit('save', credential);
}

function onHide() {
  emit('update:modelValue', false);
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      // the secrets are never sent back by the server: they are typed again
      form.value = props.credential
        ? { ...empty(), opalUrl: props.credential.opalUrl, username: props.credential.username || '' }
        : empty();
      confirm.value = '';
    }
    showDialog.value = value;
  },
);
</script>
