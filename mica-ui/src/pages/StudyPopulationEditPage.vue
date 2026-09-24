<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t(target.labels.title)" :to="target.listRoute" />
        <q-breadcrumbs-el :label="id" :to="`${target.routeBase}/${id}`" />
        <q-breadcrumbs-el :label="t('study.populations')" :to="backRoute" />
        <q-breadcrumbs-el :label="title" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <q-spinner-dots v-if="loading" color="primary" size="2em" />
      <div v-else-if="item">
        <div v-if="population && isDce" class="text-caption text-grey-7 q-mb-md">
          {{ t('study.population') }}: {{ localized(population.name, locale) || population.id }}
        </div>
        <entity-json-form
          ref="form"
          v-model="model"
          :form-path="isDce ? '/config/data-collection-event/form' : '/config/population/form'"
          :additional-errors="idErrors"
        />
        <div v-if="invalid" class="text-negative q-mt-sm">{{ t('missing_required_fields') }}</div>
        <document-save-bar
          with-comment
          :comment-required="commentRequired"
          :saving="saving"
          @save="onSave"
          @cancel="router.replace(backRoute)"
        />
      </div>
      <div v-else>
        {{ t('document.not_found') }}
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import type { ErrorObject } from 'ajv';
import type { PopulationDto, PopulationDto_DataCollectionEventDto as DceDto, StudyDto } from 'src/models/Mica';
import DocumentSaveBar from 'src/components/documents/DocumentSaveBar.vue';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import { DCE_FIELDS, POPULATION_FIELDS, fromModel, toModel, type FormModel } from 'src/composables/useDocumentModel';
import { conflictError, notifyPotentialConflicts } from 'src/composables/useDocumentActions';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import { notifyError } from 'src/utils/notify';
import { localized } from 'src/utils/persons';
import { byWeight, isValidId, nextId } from 'src/utils/studies';

type Item = PopulationDto | DceDto;

const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const route = useRoute();
const router = useRouter();
const { t, locale } = useI18n();

const id = computed(() => route.params.id as string);
const pid = computed(() => route.params.pid as string | undefined);
const dceId = computed(() => route.params.dceId as string | undefined);
/** a data collection event of the population, else the population */
const isDce = computed(() => route.meta.studyPart === 'dce');
const target = computed(() => documentTarget('individual-study', id.value));
const fields = computed(() => (isDce.value ? DCE_FIELDS : POPULATION_FIELDS));

const form = ref<InstanceType<typeof EntityJsonForm>>();
const loading = ref(true);
const saving = ref(false);
const invalid = ref(false);
const study = ref<StudyDto>();
const population = ref<PopulationDto>();
/** the population or event edited, a new one has no id in its list */
const item = ref<Item>();
const model = ref<FormModel>({});
const snapshot = ref('');
const saved = ref(false);

const commentRequired = computed(() => systemStore.configuration.isCommentsRequiredOnDocumentSave === true);
const dirty = computed(() => !saved.value && JSON.stringify(model.value) !== snapshot.value);
useConfirmLeave(dirty);

const title = computed(() => {
  const existing = isDce.value ? dceId.value : pid.value;
  if (existing) return `${t(isDce.value ? 'study.edit_dce' : 'study.edit_population')} [${existing}]`;
  return t(isDce.value ? 'study.add_dce' : 'study.add_population');
});

/** the ids of the other populations, or of the other events of the population */
const siblingIds = computed(() => {
  const items: Item[] = (isDce.value ? population.value?.dataCollectionEvents : study.value?.populations) ?? [];
  const current = isDce.value ? dceId.value : pid.value;
  return items.map((sibling) => sibling.id ?? '').filter((sibling) => sibling !== current);
});

const idErrors = computed<ErrorObject[]>(() => {
  const value = typeof model.value._id === 'string' ? model.value._id : '';
  if (value === '') return [];
  // the message is an i18n key, the form translates it
  const message = !isValidId(value) ? 'study.invalid_id' : siblingIds.value.includes(value) ? 'study.unique_id' : '';
  if (!message) return [];
  // JSON Forms validates with ajv 8 (`instancePath`), the `ajv` types resolved here are the v6 ones
  return [{ instancePath: '/_id', schemaPath: '', keyword: 'id', params: {}, message } as unknown as ErrorObject];
});

const backRoute = computed(() => {
  const query = new URLSearchParams();
  const populationId = isDce.value ? pid.value : (item.value?.id ?? pid.value);
  if (populationId) query.set('population', populationId);
  if (isDce.value && item.value?.id) query.set('dce', item.value.id);
  return `${target.value.routeBase}/${id.value}/populations${query.size > 0 ? `?${query}` : ''}`;
});

function newItem(siblings: Item[]): Item {
  const common = { id: nextId(byWeight(siblings).map((sibling) => sibling.id ?? '')), name: [], description: [] };
  const weight = siblings.length;
  return isDce.value
    ? ({ ...common, attachments: [], weight } as unknown as DceDto)
    : { ...common, dataCollectionEvents: [], weight };
}

async function initialize() {
  loading.value = true;
  try {
    const loaded = (await documentsStore.fetchDocument(target.value)).document as StudyDto;
    study.value = loaded;
    population.value = loaded.populations?.find((candidate) => candidate.id === pid.value);
    if (isDce.value) {
      const events = population.value?.dataCollectionEvents ?? [];
      item.value = !population.value
        ? undefined
        : dceId.value
          ? events.find((event) => event.id === dceId.value)
          : newItem(events);
    } else {
      item.value = pid.value ? population.value : newItem(loaded.populations ?? []);
    }
    model.value = item.value ? toModel(item.value, fields.value) : {};
    snapshot.value = JSON.stringify(model.value);
  } catch (error) {
    item.value = undefined;
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

/** the study with the edited item replaced (or added) */
function updatedStudy(current: StudyDto, updated: Item): StudyDto {
  const replace = <T extends Item>(items: T[], previousId: string | undefined, value: T) =>
    previousId ? items.map((candidate) => (candidate.id === previousId ? value : candidate)) : [...items, value];
  if (!isDce.value) {
    return { ...current, populations: replace(current.populations ?? [], pid.value, updated as PopulationDto) };
  }
  return {
    ...current,
    populations: (current.populations ?? []).map((candidate) =>
      candidate.id === pid.value
        ? {
            ...candidate,
            dataCollectionEvents: replace(candidate.dataCollectionEvents ?? [], dceId.value, updated as DceDto),
          }
        : candidate,
    ),
  };
}

async function onSave(comment: string | undefined) {
  const current = study.value;
  if (!current || !item.value) return;
  invalid.value = !form.value?.validate() || idErrors.value.length > 0;
  if (invalid.value) return;
  const updated = fromModel(item.value, model.value, fields.value);
  saving.value = true;
  try {
    notifyPotentialConflicts(await documentsStore.saveDocument(target.value, updatedStudy(current, updated), comment));
    item.value = updated;
    saved.value = true;
    await router.replace(backRoute.value);
  } catch (error) {
    notifyError(conflictError(error, 'study.population_conflict'));
  } finally {
    saving.value = false;
  }
}

onMounted(initialize);
</script>
