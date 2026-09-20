<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t(target.labels.title)" :to="target.listRoute" />
        <q-breadcrumbs-el v-if="id" :label="id" :to="`${target.routeBase}/${id}`" />
        <q-breadcrumbs-el :label="id ? t('edit') : t(target.labels.new)" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-spinner-dots v-if="loading" color="primary" size="2em" />
      <div v-else-if="document">
        <document-logo-input
          v-if="target.withLogo"
          v-model="logo"
          :document-path="target.path"
          :disable="saving"
          class="q-mb-md"
        />
        <entity-json-form ref="form" v-model="model" :form-path="target.formPath" />
        <div v-if="invalid" class="text-negative q-mt-sm">{{ t('missing_required_fields') }}</div>
        <document-save-bar
          :with-comment="!!id"
          :comment-required="commentRequired"
          :saving="saving"
          @save="onSave"
          @cancel="onCancel"
        />
      </div>
      <div v-else>
        {{ t('document.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import { onBeforeRouteLeave } from 'vue-router';
import { useQuasar } from 'quasar';
import type { AttachmentDto } from 'src/models/Mica';
import DocumentLogoInput from 'src/components/documents/DocumentLogoInput.vue';
import DocumentSaveBar from 'src/components/documents/DocumentSaveBar.vue';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import { useDocumentTarget, useRouteDocumentType } from 'src/composables/useDocumentTarget';
import { useDocumentModel, type FormModel } from 'src/composables/useDocumentModel';
import type { DocumentDto } from 'src/stores/documents';
import { notifyError } from 'src/utils/notify';

const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { t } = useI18n();

const type = useRouteDocumentType();
/** undefined for a new document */
const id = computed(() => route.params.id as string | undefined);
const { target } = useDocumentTarget(type, () => id.value ?? '');
const { toModel, fromModel } = useDocumentModel(type);

const form = ref<InstanceType<typeof EntityJsonForm>>();
const loading = ref(true);
const saving = ref(false);
const invalid = ref(false);
const document = ref<DocumentDto>();
const model = ref<FormModel>({});
const logo = ref<AttachmentDto>();
/** snapshot of what was loaded, for the dirty check */
const snapshot = ref('');
let saved = false;

const commentRequired = computed(() => systemStore.configuration.isCommentsRequiredOnDocumentSave === true);
const isDirty = computed(() => snapshotOf() !== snapshot.value);

function snapshotOf() {
  return JSON.stringify({ model: model.value, logo: logo.value?.id });
}

async function initialize() {
  loading.value = true;
  try {
    const loaded = id.value
      ? (await documentsStore.fetchDocument(target.value)).document
      : documentsStore.newDocument(type.value);
    document.value = loaded;
    model.value = toModel(loaded);
    logo.value = 'logo' in loaded ? loaded.logo : undefined;
    snapshot.value = snapshotOf();
  } catch (error) {
    document.value = undefined;
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

async function onSave(comment: string | undefined) {
  const current = document.value;
  if (!current) return;
  invalid.value = !form.value?.validate();
  if (invalid.value) return;
  const dto = fromModel(current, model.value);
  if (target.value.withLogo) {
    const withLogo = dto as { logo?: AttachmentDto | undefined };
    if (logo.value) {
      withLogo.logo = logo.value;
    } else {
      delete withLogo.logo;
    }
  }
  saving.value = true;
  try {
    const documentId = id.value
      ? (await documentsStore.saveDocument(target.value, dto, comment), id.value)
      : await documentsStore.createDocument(target.value, dto);
    saved = true;
    await router.replace(`${target.value.routeBase}/${documentId}`);
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
}

function onCancel() {
  router.replace(id.value ? `${target.value.routeBase}/${id.value}` : target.value.listRoute);
}

onBeforeRouteLeave(() => {
  if (saved || !isDirty.value) return true;
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
});

onMounted(initialize);
</script>
