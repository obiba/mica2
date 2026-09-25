<template>
  <q-dialog v-model="show" persistent @before-show="initialize">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ title }}</div>
        <div v-if="population && isDce" class="text-caption text-grey-7">
          {{ t('study.population') }}: {{ localized(population.name, locale) || population.id }}
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section style="max-height: 70vh" class="scroll">
        <entity-json-form
          v-if="item"
          ref="form"
          v-model="model"
          :form-path="isDce ? '/config/data-collection-event/form' : '/config/population/form'"
          :additional-errors="idErrors"
        />
        <div v-else>
          {{ t('document.not_found') }}
        </div>
        <div v-if="invalid" class="text-negative q-mt-sm">{{ t('missing_required_fields') }}</div>
      </q-card-section>
      <q-separator />
      <document-save-bar
        with-comment
        :comment-required="commentRequired"
        :saving="saving"
        @save="onSave"
        @cancel="onCancel"
      />
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { ErrorObject } from 'ajv';
import type { PopulationDto, PopulationDto_DataCollectionEventDto as DceDto, StudyDto } from 'src/models/Mica';
import DocumentSaveBar from 'src/components/documents/DocumentSaveBar.vue';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import {
  DCE_FIELDS,
  POPULATION_FIELDS,
  fromModel,
  modelSnapshot,
  toModel,
  type FormModel,
} from 'src/composables/useDocumentModel';
import { conflictError, notifyPotentialConflicts } from 'src/composables/useDocumentActions';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import { notifyError } from 'src/utils/notify';
import { localized } from 'src/utils/persons';
import { byWeight, isValidId, nextId } from 'src/utils/studies';

type Item = PopulationDto | DceDto;

interface Props {
  study: StudyDto;
  /** the population edited, or the one of the event edited */
  populationId?: string | undefined;
  /** the event edited, when editing an event */
  dceId?: string | undefined;
  /** a data collection event of the population, else the population */
  isDce?: boolean;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ saved: [populationId: string, dceId?: string] }>();
const documentsStore = useDocumentsStore();
const systemStore = useSystemStore();
const { t, locale } = useI18n();

const target = computed(() => documentTarget('individual-study', props.study.id ?? ''));
const fields = computed(() => (props.isDce ? DCE_FIELDS : POPULATION_FIELDS));
/** the id of the item edited, undefined for a new one */
const existingId = computed(() => (props.isDce ? props.dceId : props.populationId));

const form = ref<InstanceType<typeof EntityJsonForm>>();
const saving = ref(false);
const invalid = ref(false);
const population = computed(() => props.study.populations?.find((candidate) => candidate.id === props.populationId));
/** the population or event edited, a new one has no id in its list */
const item = ref<Item>();
const model = ref<FormModel>({});
const snapshot = ref('');

const commentRequired = computed(() => systemStore.configuration.isCommentsRequiredOnDocumentSave === true);
const dirty = computed(() => show.value && modelSnapshot(model.value) !== snapshot.value);
const { confirmLeave } = useConfirmLeave(dirty);

const title = computed(() => {
  if (existingId.value) {
    return `${t(props.isDce ? 'study.edit_dce' : 'study.edit_population')} [${existingId.value}]`;
  }
  return t(props.isDce ? 'study.add_dce' : 'study.add_population');
});

/** the populations, or the events of the population */
const siblings = computed<Item[]>(
  () => (props.isDce ? population.value?.dataCollectionEvents : props.study.populations) ?? [],
);

/** the ids of the other populations, or of the other events of the population */
const siblingIds = computed(() =>
  siblings.value.map((sibling) => sibling.id ?? '').filter((sibling) => sibling !== existingId.value),
);

const idErrors = computed<ErrorObject[]>(() => {
  const value = typeof model.value._id === 'string' ? model.value._id : '';
  if (value === '') return [];
  // the message is an i18n key, the form translates it
  const message = !isValidId(value) ? 'study.invalid_id' : siblingIds.value.includes(value) ? 'study.unique_id' : '';
  if (!message) return [];
  // JSON Forms validates with ajv 8 (`instancePath`), the `ajv` types resolved here are the v6 ones
  return [{ instancePath: '/_id', schemaPath: '', keyword: 'id', params: {}, message } as unknown as ErrorObject];
});

function newItem(): Item {
  const common = {
    id: nextId(byWeight(siblings.value).map((sibling) => sibling.id ?? '')),
    name: [],
    description: [],
  };
  const weight = siblings.value.length;
  return props.isDce
    ? ({ ...common, attachments: [], weight } as unknown as DceDto)
    : { ...common, dataCollectionEvents: [], weight };
}

function initialize() {
  invalid.value = false;
  if (props.isDce && !population.value) {
    item.value = undefined;
  } else {
    item.value = existingId.value ? siblings.value.find((sibling) => sibling.id === existingId.value) : newItem();
  }
  model.value = item.value ? toModel(item.value, fields.value) : {};
  snapshot.value = modelSnapshot(model.value);
}

/** the study with the edited item replaced (or added) */
function updatedStudy(current: StudyDto, updated: Item): StudyDto {
  const replace = <T extends Item>(items: T[], previousId: string | undefined, value: T) =>
    previousId ? items.map((candidate) => (candidate.id === previousId ? value : candidate)) : [...items, value];
  if (!props.isDce) {
    return {
      ...current,
      populations: replace(current.populations ?? [], props.populationId, updated as PopulationDto),
    };
  }
  return {
    ...current,
    populations: (current.populations ?? []).map((candidate) =>
      candidate.id === props.populationId
        ? {
            ...candidate,
            dataCollectionEvents: replace(candidate.dataCollectionEvents ?? [], props.dceId, updated as DceDto),
          }
        : candidate,
    ),
  };
}

async function onSave(comment: string | undefined) {
  if (!item.value) return;
  invalid.value = !form.value?.validate() || idErrors.value.length > 0;
  if (invalid.value) return;
  const updated = fromModel(item.value, model.value, fields.value);
  saving.value = true;
  try {
    notifyPotentialConflicts(
      await documentsStore.saveDocument(target.value, updatedStudy(props.study, updated), comment),
    );
    show.value = false;
    if (props.isDce) emit('saved', props.populationId ?? '', updated.id);
    else emit('saved', updated.id ?? '');
  } catch (error) {
    notifyError(conflictError(error, 'study.population_conflict'));
  } finally {
    saving.value = false;
  }
}

async function onCancel() {
  if (await confirmLeave()) show.value = false;
}
</script>
