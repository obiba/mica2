<template>
  <div>
    <div class="text-h6 q-mb-xs">{{ t('config.encryption_keys') }}</div>
    <div class="text-hint q-mb-sm">{{ t('config.encryption_keys_help') }}</div>
    <q-btn-dropdown
      v-if="authStore.isAdministrator"
      color="primary"
      icon="add"
      :label="t('config.add_key_pair')"
      size="sm"
      class="q-mr-sm"
    >
      <q-list>
        <q-item clickable v-close-popup @click="onAdd('create')">
          <q-item-section>{{ t('config.keys.create') }}</q-item-section>
        </q-item>
        <q-item clickable v-close-popup @click="onAdd('import')">
          <q-item-section>{{ t('config.keys.import') }}</q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>
    <q-btn
      outline
      color="primary"
      icon="download"
      :label="t('config.download_certificate')"
      size="sm"
      type="a"
      :href="systemCertificateUrl()"
      target="_blank"
    />
    <key-pair-dialog v-model="showDialog" :mode="mode" :saving="saving" @save="onSave" />
  </div>
</template>

<script setup lang="ts">
import KeyPairDialog, { type KeyPairMode } from 'src/components/settings/general/KeyPairDialog.vue';
import { saveSystemKey, systemCertificateUrl } from 'src/composables/useKeyStore';
import type { KeyForm } from 'src/models/Mica';
import { notifyError, notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const authStore = useAuthStore();

const showDialog = ref(false);
const mode = ref<KeyPairMode>('create');
const saving = ref(false);

function onAdd(value: KeyPairMode) {
  mode.value = value;
  showDialog.value = true;
}

async function onSave(keyForm: KeyForm) {
  saving.value = true;
  try {
    await saveSystemKey(keyForm);
    notifySuccess('config.keys.saved');
    showDialog.value = false;
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}
</script>
