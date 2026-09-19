<template>
  <q-card flat bordered class="q-mb-md">
    <q-card-section class="row items-center q-py-sm bg-grey-2">
      <div class="text-subtitle1">{{ title }}</div>
      <q-space />
      <q-btn
        v-if="authStore.isAdministrator"
        flat
        dense
        round
        size="sm"
        icon="edit"
        color="primary"
        :title="t('edit')"
        @click="onEdit"
      />
    </q-card-section>
    <q-separator />
    <q-card-section v-if="help" class="text-hint q-pb-none">{{ help }}</q-card-section>
    <fields-list :items="items" :dbobject="systemStore.configuration" max-width="320" />

    <q-dialog v-model="showDialog" persistent>
      <q-card class="dialog-md">
        <q-form @submit="onSave">
          <q-card-section>
            <div class="text-h6">{{ title }}</div>
          </q-card-section>
          <q-separator />
          <q-card-section v-if="form" style="max-height: 70vh" class="scroll">
            <slot name="form" :form="form" />
          </q-card-section>
          <q-separator />
          <q-card-actions align="right" class="bg-grey-3">
            <q-btn flat :label="t('cancel')" color="secondary" :disable="saving" v-close-popup />
            <q-btn flat type="submit" :label="t('save')" color="primary" :loading="saving" />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>
  </q-card>
</template>

<script setup lang="ts">
import FieldsList, { type FieldItem } from 'src/components/FieldsList.vue';
import type { MicaConfigDto } from 'src/models/Mica';
import { applyFeatureDependencies, copyConfig } from 'src/utils/config';
import { notifyError, notifySuccess } from 'src/utils/notify';

interface Props {
  title: string;
  help?: string;
  /** the fields shown, from the current configuration */
  items: FieldItem[];
}

defineProps<Props>();
defineSlots<{
  /** the fields edited, bound to a working copy of the configuration */
  form(props: { form: MicaConfigDto }): unknown;
}>();

const { t } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();

const showDialog = ref(false);
const saving = ref(false);
/** a working copy of the configuration, discarded on cancel */
const form = ref<MicaConfigDto>();

function onEdit() {
  form.value = copyConfig(systemStore.configuration);
  showDialog.value = true;
}

/** the whole configuration is replaced, with the fields of this section changed */
async function onSave() {
  if (!form.value) return;
  saving.value = true;
  try {
    await systemStore.save(applyFeatureDependencies(form.value));
    notifySuccess('config.saved');
    showDialog.value = false;
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}
</script>
