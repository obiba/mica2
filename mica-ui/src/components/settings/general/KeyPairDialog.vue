<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-md">
      <q-form @submit="onSave" @validation-error="onValidationError">
        <q-card-section>
          <div class="text-h6">
            {{ mode === 'create' ? t('config.keys.title_create') : t('config.keys.title_import') }}
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section style="max-height: 70vh" class="scroll">
          <q-input
            v-if="withOpalUrl"
            v-model="opalUrl"
            dense
            outlined
            type="url"
            :label="t('config.opal_url') + ' *'"
            :hint="t('config.keys.opal_url_help')"
            :rules="[(value: string) => /^https:\/\/.+/.test(value || '') || t('config.keys.opal_url_https')]"
            lazy-rules
            class="q-mb-md"
          />
          <template v-if="mode === 'create'">
            <q-input v-model="publicForm.name" dense outlined :label="t('config.keys.name')" class="q-mb-md" />
            <q-input
              v-model="publicForm.organization"
              dense
              outlined
              :label="t('config.keys.organization')"
              class="q-mb-md"
            />
            <q-input
              v-model="publicForm.organizationalUnit"
              dense
              outlined
              :label="t('config.keys.organizational_unit')"
              class="q-mb-md"
            />
            <q-input v-model="publicForm.locality" dense outlined :label="t('config.keys.locality')" class="q-mb-md" />
            <q-input v-model="publicForm.state" dense outlined :label="t('config.keys.state')" class="q-mb-md" />
            <q-input v-model="publicForm.country" dense outlined :label="t('config.keys.country')" class="q-mb-md" />
            <q-expansion-item
              v-model="advancedExpanded"
              dense
              :label="t('config.keys.advanced')"
              header-class="text-primary q-px-none"
            >
              <q-input
                v-model="privateForm.algo"
                dense
                outlined
                :label="t('config.keys.algo') + ' *'"
                :rules="[required]"
                lazy-rules
                class="q-mb-md q-mt-sm"
              />
              <q-input
                v-model.number="privateForm.size"
                dense
                outlined
                type="number"
                :label="t('config.keys.size') + ' *'"
                :rules="[
                  (value: number) => (value !== undefined && value !== null && value > 0) || t('number_invalid'),
                ]"
                lazy-rules
                class="q-mb-md"
              />
            </q-expansion-item>
          </template>
          <template v-else>
            <q-input
              v-model="privateImport"
              dense
              outlined
              type="textarea"
              rows="6"
              :label="t('config.keys.private') + ' *'"
              :rules="[required]"
              lazy-rules
              class="q-mb-md"
            />
            <q-input
              v-model="publicImport"
              dense
              outlined
              type="textarea"
              rows="6"
              :label="t('config.keys.public') + ' *'"
              :rules="[required]"
              lazy-rules
              class="q-mb-md"
            />
          </template>
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
import { KeyType, type KeyForm, type PrivateKeyForm, type PublicKeyForm } from 'src/models/Mica';

export type KeyPairMode = 'create' | 'import';

interface Props {
  modelValue: boolean;
  /** create a self-signed key pair from its subject, or import PEM keys */
  mode: KeyPairMode;
  /** the key pair is an Opal credential: ask for the Opal URL */
  withOpalUrl?: boolean;
  saving?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; save: [keyForm: KeyForm, opalUrl: string] }>();
const { t } = useI18n();

const showDialog = ref(props.modelValue);
const opalUrl = ref('');
const publicForm = ref<PublicKeyForm>({});
const privateForm = ref<PrivateKeyForm>({ algo: 'RSA', size: 2048 });
const privateImport = ref('');
const publicImport = ref('');
const advancedExpanded = ref(false);

const required = (value: string) => !!value?.trim() || t('required');

function reset() {
  opalUrl.value = '';
  publicForm.value = {};
  privateForm.value = { algo: 'RSA', size: 2048 };
  advancedExpanded.value = false;
  privateImport.value = '';
  publicImport.value = '';
}

function onSave() {
  const keyForm: KeyForm =
    props.mode === 'create'
      ? { keyType: KeyType.KEY_PAIR, privateForm: privateForm.value, publicForm: publicForm.value }
      : { keyType: KeyType.KEY_PAIR, privateImport: privateImport.value, publicImport: publicImport.value };
  emit('save', keyForm, opalUrl.value.trim());
}

/** an invalid field hidden in the collapsed advanced options must be shown to be fixed */
function onValidationError() {
  const { algo, size } = privateForm.value;
  if (!algo?.trim() || !(size > 0)) advancedExpanded.value = true;
}

function onHide() {
  emit('update:modelValue', false);
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) reset();
    showDialog.value = value;
  },
);
</script>
