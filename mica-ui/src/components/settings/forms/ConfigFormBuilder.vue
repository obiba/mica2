<template>
  <div>
    <p class="text-grey-8" v-html="info" />
    <q-banner v-if="state.legacy && legacyMessage" rounded class="bg-warning text-dark q-mb-md">
      <template #avatar>
        <q-icon name="warning" />
      </template>
      {{ legacyMessage }}
    </q-banner>
    <q-spinner-dots v-if="state.loading && !state.form" color="primary" size="2em" />
    <div v-else-if="state.form">
      <q-json-form-builder
        ref="builder"
        :model-value="state.form"
        :languages="languages"
        :locale="locale"
        @update:model-value="state.update"
      />
      <div class="q-mt-md row items-center q-gutter-sm">
        <q-btn color="primary" :label="t('save')" :loading="state.saving" :disable="!state.dirty" @click="onSave" />
        <q-btn flat color="primary" :label="t('cancel')" :disable="!state.dirty || state.saving" @click="state.load" />
        <template v-if="state.source.revisions">
          <q-btn
            color="secondary"
            :label="t('config.form_publish')"
            :loading="state.publishing"
            :disable="!state.canPublish || state.dirty || state.saving"
            @click="onPublish"
          >
            <q-tooltip>{{ t('config.form_publish_help') }}</q-tooltip>
          </q-btn>
          <span class="text-caption text-grey-7">{{ publishedLabel }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { QJsonFormBuilder } from '@obiba/quasar-ui-json-form/builder';
import type { FormModel } from '@obiba/quasar-ui-json-form/builder';
import type { ConfigFormState } from 'src/composables/useConfigForm';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import { getDateLabel } from 'src/utils/dates';
import { notifySuccess } from 'src/utils/notify';

interface Props {
  /** the form configuration edited, from `useConfigForm` (or one of its wrappers) */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  state: ConfigFormState<any>;
  /** what the form is about, may contain HTML from the app bundles */
  info: string;
  /** shown when the loaded form was in the legacy dialect: what to do with it */
  legacyMessage?: string;
}

const props = defineProps<Props>();
const { t, locale } = useI18n({ useScope: 'global' });
const systemStore = useSystemStore();

const languages = computed(() => systemStore.languages);
// the refs of the composable, unwrapped for the template
const state = reactive(props.state);

const builder = ref<{ getModel: () => FormModel }>();

const publishedLabel = computed(() =>
  state.published
    ? t('config.form_published', {
        date: getDateLabel(state.published.lastUpdateDate),
        revision: state.published.revision,
      })
    : t('config.form_not_published'),
);

async function onSave() {
  const model = builder.value?.getModel();
  if (!model) return;
  if (await state.save(model)) notifySuccess(t('config.form_saved'));
}

async function onPublish() {
  if (await state.publish()) notifySuccess(t('config.form_published_ok'));
}

const { confirmLeave } = useConfirmLeave(props.state.dirty);

defineExpose({ confirmLeave });

onMounted(state.load);
</script>
