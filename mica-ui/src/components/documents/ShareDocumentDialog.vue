<template>
  <q-dialog v-model="showDialog" @show="refresh">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('document.share_title') }}</div>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-gutter-md">
        <q-input :model-value="link" :label="t('document.share_link')" :hint="t('document.share_link_hint')" readonly>
          <template #append>
            <q-btn flat dense icon="content_copy" :disable="!link" :title="t('copy')" @click="copy" />
          </template>
        </q-input>
        <q-input
          v-model="expire"
          type="date"
          :label="t('document.share_expire')"
          :hint="t('document.share_expire_hint')"
          stack-label
          clearable
          @update:model-value="refresh"
        />
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('close')" color="secondary" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { isAxiosError } from 'axios';
import { copyToClipboard, date } from 'quasar';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { notifyError, notifySuccess } from 'src/utils/notify';

interface Props {
  target: DocumentTarget;
}

const props = defineProps<Props>();
const showDialog = defineModel<boolean>({ required: true });
const { t } = useI18n();
const documentsStore = useDocumentsStore();

const link = ref('');
const expire = ref<string | null>(date.formatDate(date.addToDate(new Date(), { months: 1 }), 'YYYY-MM-DD'));

async function refresh() {
  link.value = '';
  try {
    link.value = await documentsStore.shareDocument(props.target, expire.value || undefined);
  } catch (error) {
    // no portal URL to build the link from
    notifyError(isAxiosError(error) && error.response?.status === 409 ? 'document.share_no_portal_url' : error);
  }
}

async function copy() {
  await copyToClipboard(link.value);
  notifySuccess('document.link_copied');
}
</script>
