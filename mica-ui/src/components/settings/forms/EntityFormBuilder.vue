<template>
  <div>
    <p class="text-grey-8" v-html="info" />
    <q-spinner-dots v-if="loading && !form" color="primary" size="2em" />
    <div v-else-if="form">
      <q-json-form-builder
        ref="builder"
        :model-value="form"
        :languages="languages"
        :locale="locale"
        @update:model-value="update"
      />
      <div class="q-mt-md q-gutter-sm">
        <q-btn color="primary" :label="t('save')" :loading="saving" :disable="!dirty" @click="onSave" />
        <q-btn flat color="primary" :label="t('cancel')" :disable="!dirty || saving" @click="load" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeRouteLeave } from 'vue-router';
import { useQuasar } from 'quasar';
import { QJsonFormBuilder } from '@obiba/quasar-ui-json-form/builder';
import type { FormModel } from '@obiba/quasar-ui-json-form/builder';
import { useEntityConfigForm, type EntityConfigTarget } from 'src/composables/useEntityConfigForm';
import { notifySuccess } from 'src/utils/notify';

interface Props {
  /** the form configuration edited: `network`... */
  target: EntityConfigTarget;
  /** what the form is about, may contain HTML from the app bundles */
  info: string;
}

const props = defineProps<Props>();
const { t, locale } = useI18n({ useScope: 'global' });
const $q = useQuasar();
const systemStore = useSystemStore();

const languages = computed(() => systemStore.languages);
const { loading, saving, form, dirty, load, update, save } = useEntityConfigForm(props.target);

const builder = ref<{ getModel: () => FormModel }>();

async function onSave() {
  const model = builder.value?.getModel();
  if (!model) return;
  if (await save(model)) notifySuccess(t('config.form_saved'));
}

/** true when the form can be left: nothing to save, or the user gives up the unsaved changes */
function confirmLeave(): Promise<boolean> {
  if (!dirty.value) return Promise.resolve(true);
  return new Promise<boolean>((resolve) => {
    $q.dialog({
      title: t('document.unsaved_title'),
      message: t('document.unsaved_text'),
      cancel: true,
      persistent: true,
    })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false));
  });
}

onBeforeRouteLeave(confirmLeave);

defineExpose({ confirmLeave });

onMounted(load);
</script>
