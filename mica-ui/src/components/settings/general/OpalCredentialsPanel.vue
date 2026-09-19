<template>
  <div>
    <div class="text-h6 q-mb-xs">{{ t('config.opal_credentials') }}</div>
    <div class="text-hint q-mb-sm" v-html="t('config.opal_credentials_help')"></div>
    <q-btn-dropdown
      v-if="authStore.isAdministrator"
      color="primary"
      icon="add"
      :label="t('config.opal_credential_add')"
      size="sm"
      class="q-mb-sm"
    >
      <q-list>
        <q-item clickable v-close-popup @click="onAddKeyPair('create')">
          <q-item-section>{{ t('config.keys.create') }}</q-item-section>
        </q-item>
        <q-item clickable v-close-popup @click="onAddKeyPair('import')">
          <q-item-section>{{ t('config.keys.import') }}</q-item-section>
        </q-item>
        <q-item clickable v-close-popup @click="onAddCredential('USERNAME')">
          <q-item-section>{{ t('config.username') }}</q-item-section>
        </q-item>
        <q-item clickable v-close-popup @click="onAddCredential('TOKEN')">
          <q-item-section>{{ t('config.opal_token') }}</q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>
    <q-table
      flat
      dense
      :rows="credentials"
      :columns="columns"
      row-key="opalUrl"
      :loading="loading"
      :pagination="{ rowsPerPage: 20 }"
      :rows-per-page-options="[20, 50]"
      :no-data-label="t('config.opal_credentials_none')"
      :hide-pagination="credentials.length <= 20"
    >
      <template v-slot:body-cell-opalUrl="props">
        <q-td :props="props">
          <a :href="props.value" target="_blank">{{ props.value }}</a>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props" class="text-no-wrap">
          <q-btn
            v-if="props.row.type === 'USERNAME' || props.row.type === 'TOKEN'"
            flat
            dense
            round
            size="sm"
            icon="edit"
            color="primary"
            :title="t('edit')"
            @click="onEditCredential(props.row)"
          />
          <q-btn
            v-if="props.row.type === 'PUBLIC_KEY_CERTIFICATE'"
            flat
            dense
            round
            size="sm"
            icon="download"
            color="primary"
            :title="t('config.download_certificate')"
            type="a"
            :href="opalCredentialCertificateUrl(props.row.opalUrl)"
            target="_blank"
          />
          <q-btn
            flat
            dense
            round
            size="sm"
            icon="delete"
            color="negative"
            :title="t('delete')"
            @click="onDeleteRequest(props.row)"
          />
        </q-td>
      </template>
    </q-table>
    <key-pair-dialog v-model="showKeyPair" :mode="keyPairMode" with-opal-url :saving="saving" @save="onSaveKeyPair" />
    <opal-credential-dialog
      v-model="showCredential"
      :kind="credentialKind"
      :credential="edited"
      :saving="saving"
      @save="onSaveCredential"
    />
    <confirm-dialog
      v-model="showDelete"
      :title="t('config.opal_credential_delete_title')"
      :text="t('config.opal_credential_delete_text', { url: toDelete?.opalUrl || '' })"
      @confirm="onDelete"
    />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import KeyPairDialog, { type KeyPairMode } from 'src/components/settings/general/KeyPairDialog.vue';
import OpalCredentialDialog from 'src/components/settings/general/OpalCredentialDialog.vue';
import { opalCredentialCertificateUrl, useOpalCredentials } from 'src/composables/useOpalCredentials';
import { OpalCredentialType, type KeyForm, type OpalCredentialDto } from 'src/models/Mica';

const { t } = useI18n();
const authStore = useAuthStore();
const { credentials, loading, load, save, remove } = useOpalCredentials();

const saving = ref(false);
const showKeyPair = ref(false);
const keyPairMode = ref<KeyPairMode>('create');
const showCredential = ref(false);
const credentialKind = ref<'USERNAME' | 'TOKEN'>('USERNAME');
const edited = ref<OpalCredentialDto>();
const showDelete = ref(false);
const toDelete = ref<OpalCredentialDto>();

const columns = computed<QTableColumn[]>(() => [
  { name: 'opalUrl', label: t('config.opal_url'), field: 'opalUrl', align: 'left', sortable: true },
  {
    name: 'type',
    label: t('type'),
    field: 'type',
    align: 'left',
    format: (value: string) => t(`config.opal_credential_type.${value}`),
  },
  ...(authStore.isAdministrator
    ? [{ name: 'actions', label: t('history.actions'), field: 'opalUrl', align: 'left' } as QTableColumn]
    : []),
]);

function onAddKeyPair(mode: KeyPairMode) {
  keyPairMode.value = mode;
  showKeyPair.value = true;
}

function onAddCredential(kind: 'USERNAME' | 'TOKEN') {
  credentialKind.value = kind;
  edited.value = undefined;
  showCredential.value = true;
}

function onEditCredential(credential: OpalCredentialDto) {
  credentialKind.value = credential.type === OpalCredentialType.TOKEN ? 'TOKEN' : 'USERNAME';
  edited.value = credential;
  showCredential.value = true;
}

async function saveCredential(credential: OpalCredentialDto): Promise<boolean> {
  saving.value = true;
  try {
    return await save(credential);
  } finally {
    saving.value = false;
  }
}

async function onSaveKeyPair(keyForm: KeyForm, opalUrl: string) {
  if (await saveCredential({ type: OpalCredentialType.PUBLIC_KEY_CERTIFICATE, opalUrl, keyForm }))
    showKeyPair.value = false;
}

async function onSaveCredential(credential: OpalCredentialDto) {
  if (await saveCredential(credential)) showCredential.value = false;
}

function onDeleteRequest(credential: OpalCredentialDto) {
  toDelete.value = credential;
  showDelete.value = true;
}

async function onDelete() {
  if (toDelete.value) await remove(toDelete.value);
}

onMounted(load);
</script>
