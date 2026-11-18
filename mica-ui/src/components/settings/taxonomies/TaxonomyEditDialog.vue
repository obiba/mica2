<template>
  <q-dialog v-model="showDialog" persistent @hide="emit('update:modelValue', false)">
    <q-card class="dialog-md">
      <q-form @submit="onSave">
        <q-card-section>
          <div class="text-h6">{{ t(`taxonomies.edit.${kind}_${entity ? 'edit' : 'add'}`) }}</div>
        </q-card-section>
        <q-separator />
        <q-card-section class="q-gutter-sm scroll" style="max-height: 70vh">
          <q-banner v-if="locked" dense class="bg-grey-2 text-grey-8">{{ t('taxonomies.edit.static_info') }}</q-banner>

          <template v-if="kind === 'term' && range">
            <div class="row q-col-gutter-sm">
              <q-input
                v-model.number="term.from"
                type="number"
                dense
                :label="t('taxonomies.edit.from')"
                class="col-6"
              />
              <q-input v-model.number="term.to" type="number" dense :label="t('taxonomies.edit.to')" class="col-6" />
            </div>
          </template>
          <q-input
            v-else-if="kind !== 'taxonomy'"
            v-model="form.name"
            dense
            autofocus
            :readonly="nameLocked"
            :label="t('name') + ' *'"
            :hint="kind === 'term' ? t('taxonomies.edit.term_name_help') : t('taxonomies.edit.name_help')"
            :rules="[(value: string) => !!value?.trim() || t('required')]"
            lazy-rules
          />

          <q-input
            v-for="lang in languages"
            :key="`title-${lang}`"
            v-model="form.title[lang]"
            dense
            :label="`${t('taxonomies.title_column')} (${lang})`"
          />
          <q-input
            v-for="lang in languages"
            :key="`description-${lang}`"
            v-model="form.description[lang]"
            dense
            type="textarea"
            autogrow
            :label="`${t('description')} (${lang})`"
          />

          <template v-if="kind === 'vocabulary'">
            <div class="text-subtitle2 q-mt-md">{{ t('taxonomies.edit.definition') }}</div>
            <q-input
              v-model="voc.field"
              dense
              :readonly="locked"
              :label="t('taxonomies.edit.field') + (locked ? '' : ' *')"
              :hint="t('taxonomies.edit.field_help')"
              list="taxonomy-fields"
            />
            <datalist id="taxonomy-fields">
              <option v-for="field in fields" :key="field" :value="field" />
            </datalist>
            <q-select
              v-model="voc.type"
              :options="typeOptions"
              emit-value
              map-options
              dense
              :readonly="locked"
              :label="t('type')"
              :hint="t('taxonomies.edit.type_help')"
            />
            <q-toggle v-model="voc.repeatable" :disable="locked" :label="t('taxonomies.repeatable')" />
            <q-toggle v-model="voc.localized" :disable="locked" :label="t('taxonomies.edit.localized')" />
            <q-toggle
              v-if="isRangeType(voc.type)"
              v-model="voc.range"
              :disable="locked"
              :label="t('taxonomies.edit.range')"
            />
            <q-select
              v-if="classNames.length"
              v-model="voc.forClassName"
              :options="classOptions"
              emit-value
              map-options
              dense
              :readonly="locked"
              :label="t('taxonomies.edit.for_class_name')"
              :hint="t('taxonomies.edit.for_class_name_help')"
            />
            <q-select
              v-model="voc.termsSortKey"
              :options="sortOptions"
              emit-value
              map-options
              dense
              :readonly="locked"
              :label="t('taxonomies.edit.terms_sort_key')"
            />

            <div class="text-subtitle2 q-mt-md">{{ t('taxonomies.edit.display') }}</div>
            <q-toggle v-model="voc.hidden" :label="t('taxonomies.edit.hidden')" />
            <q-toggle v-model="voc.facet" :label="t('taxonomies.edit.facet')" />
            <div v-if="voc.facet" class="row q-col-gutter-sm items-center">
              <q-input
                v-model.number="voc.facetPosition"
                type="number"
                dense
                :label="t('taxonomies.edit.facet_position')"
                class="col-6"
              />
              <q-toggle v-model="voc.facetExpanded" :label="t('taxonomies.edit.facet_expanded')" class="col-6" />
            </div>
          </template>

          <div v-if="kind === 'term' && !range" class="text-caption text-grey-7">
            {{ t('taxonomies.edit.term_info') }}
          </div>
          <div v-if="error" class="text-negative">{{ error }}</div>
        </q-card-section>
        <q-separator />
        <q-card-actions align="right" class="bg-grey-3">
          <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
          <q-btn type="submit" :label="t('apply')" color="primary" />
        </q-card-actions>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { TaxonomyDto, TermDto, VocabularyDto } from 'src/models/Opal';
import {
  CLASS_NAMES,
  TERMS_SORT_KEYS,
  VOCABULARY_TYPES,
  fromLocaleTexts,
  getAttribute,
  isRangeType,
  isStatic,
  rangeError,
  vocabularyType,
  termForm,
  toLocaleTexts,
  toTerm,
  toVocabulary,
  vocabularyError,
  vocabularyForm,
  type TermForm,
  type VocabularyForm,
} from 'src/utils/taxonomyConfig';

type Entity = TaxonomyDto | VocabularyDto | TermDto;

interface Props {
  modelValue: boolean;
  kind: 'taxonomy' | 'vocabulary' | 'term';
  target: string;
  /** the edited item, none when adding one */
  entity?: Entity | undefined;
  /** the other items at the same level: the name must be unique */
  siblings?: Entity[];
  /** the vocabulary of the term */
  vocabulary?: VocabularyDto | undefined;
  /** the searchable fields of the documents, from the forms */
  fields?: string[];
}

const props = withDefaults(defineProps<Props>(), { siblings: () => [], fields: () => [] });
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; save: [entity: Entity] }>();
const { t } = useI18n();
const systemStore = useSystemStore();

const showDialog = ref(props.modelValue);
const voc = ref<VocabularyForm>(vocabularyForm());
const term = ref<TermForm>(termForm());
const error = ref('');

/** the texts and name of the edited item, whatever its kind */
const form = computed(() => (props.kind === 'term' ? term.value : voc.value));

const languages = computed(() => systemStore.languages);

/** the terms of a numeric range vocabulary are intervals */
const range = computed(() => props.kind === 'term' && getAttribute(props.vocabulary, 'range') === 'true');

/** the definition of a built-in vocabulary cannot change */
const locked = computed(
  () =>
    (props.kind === 'vocabulary' && isStatic(props.entity as VocabularyDto)) ||
    (props.kind === 'term' && isStatic(props.vocabulary)),
);
const nameLocked = computed(() => locked.value && !!props.entity);

const classNames = computed(() => CLASS_NAMES[props.target] || []);

const typeOptions = VOCABULARY_TYPES.map((value) => ({ value, label: t(`taxonomies.edit.types.${value}`) }));
const sortOptions = TERMS_SORT_KEYS.map((value) => ({
  value,
  label: t(`taxonomies.edit.sort_keys.${value || 'default'}`),
}));
const classOptions = computed(() => [
  { value: '', label: t('taxonomies.edit.all_classes') },
  ...classNames.value.map((value) => ({ value, label: value })),
]);

function onSave() {
  error.value = '';
  const others = props.siblings.filter((sibling) => sibling !== props.entity);
  let result: Entity;
  if (props.kind === 'taxonomy') {
    result = {
      ...(props.entity as TaxonomyDto),
      title: fromLocaleTexts(voc.value.title),
      description: fromLocaleTexts(voc.value.description),
    };
  } else if (props.kind === 'vocabulary') {
    result = toVocabulary(voc.value, props.entity as VocabularyDto | undefined);
    const key = vocabularyError(
      result as VocabularyDto,
      others as VocabularyDto[],
      props.fields,
      getAttribute(props.entity as VocabularyDto | undefined, 'field'),
    );
    if (key) error.value = t(key);
  } else {
    result = toTerm(term.value, range.value, props.entity as TermDto | undefined);
    const invalid = range.value
      ? rangeError(term.value.from, term.value.to, vocabularyType(props.vocabulary))
      : !result.name && 'taxonomies.edit.name_required';
    if (invalid) error.value = t(invalid);
    else if (others.some((other) => other.name === result.name)) error.value = t('taxonomies.edit.name_exists');
  }
  if (!error.value) {
    emit('save', result);
    showDialog.value = false;
  }
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      error.value = '';
      if (props.kind === 'term') term.value = termForm(props.entity as TermDto | undefined);
      else if (props.kind === 'vocabulary') voc.value = vocabularyForm(props.entity as VocabularyDto | undefined);
      else
        voc.value = {
          ...vocabularyForm(),
          title: toLocaleTexts(props.entity?.title),
          description: toLocaleTexts(props.entity?.description),
        };
    }
    showDialog.value = value;
  },
);
</script>
