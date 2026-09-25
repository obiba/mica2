<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-lg">
      <q-card-section>
        <div class="text-h6">{{ t('annotations.add_title') }}</div>
        <div class="text-hint">{{ t('annotations.add_help') }}</div>
      </q-card-section>
      <q-card-section class="q-pt-none">
        <q-input v-model="filter" dense clearable autofocus :placeholder="t('annotations.filter')">
          <template #prepend>
            <q-icon name="search" />
          </template>
        </q-input>
      </q-card-section>
      <q-separator />
      <div class="row no-wrap" style="height: 60vh">
        <q-list class="col-4 scroll" separator>
          <q-item
            v-for="item in shown"
            :key="item.taxonomy.name"
            clickable
            :active="item.taxonomy.name === current?.taxonomy.name"
            active-class="bg-blue-1 text-primary"
            @click="taxonomyName = item.taxonomy.name"
          >
            <q-item-section>
              <q-item-label class="text-weight-medium">{{ title(item.taxonomy) }}</q-item-label>
              <q-item-label v-if="description(item.taxonomy)" caption lines="3">
                {{ description(item.taxonomy) }}
              </q-item-label>
            </q-item-section>
            <q-item-section v-if="selectedCount(item.taxonomy.name) > 0" side top>
              <q-badge color="primary" :label="selectedCount(item.taxonomy.name)" />
            </q-item-section>
          </q-item>
          <q-item v-if="shown.length === 0">
            <q-item-section class="text-hint">{{ t('annotations.no_match') }}</q-item-section>
          </q-item>
        </q-list>
        <q-separator vertical />
        <div v-if="current" class="col scroll">
          <div class="q-pa-md bg-grey-1">
            <div class="text-subtitle1 text-weight-medium">{{ title(current.taxonomy) }}</div>
            <div v-if="description(current.taxonomy)" class="text-body2 text-grey-8">
              {{ description(current.taxonomy) }}
            </div>
          </div>
          <q-separator />
          <q-list separator>
            <q-item
              v-for="vocabulary in current.vocabularies"
              :key="vocabulary.name"
              tag="label"
              v-ripple
              :disable="annotated(current.taxonomy.name, vocabulary.name)"
            >
              <q-item-section side top>
                <q-checkbox
                  :model-value="
                    annotated(current.taxonomy.name, vocabulary.name) ||
                    isSelected(current.taxonomy.name, vocabulary.name)
                  "
                  :disable="annotated(current.taxonomy.name, vocabulary.name)"
                  @update:model-value="toggle(current.taxonomy.name, vocabulary.name)"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-weight-medium">{{ title(vocabulary) }}</q-item-label>
                <q-item-label v-if="description(vocabulary)" class="text-body2 text-grey-8">
                  {{ description(vocabulary) }}
                </q-item-label>
                <q-item-label caption>
                  <code>{{ vocabulary.name }}</code>
                  · {{ t('annotations.terms_count', vocabulary.terms?.length ?? 0) }}
                  <span v-if="annotated(current.taxonomy.name, vocabulary.name)">
                    · {{ t('annotations.annotated') }}
                  </span>
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </div>
      </div>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn
          :label="t('annotations.annotate', selected.length)"
          color="primary"
          :disable="selected.length === 0"
          v-close-popup
          @click="emit('add', selected)"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { AttributeDto } from 'src/models/Mica';
import type { TaxonomyDto, TermDto } from 'src/models/Opal';
import { isAnnotated, matchesFilter, type Annotation } from 'src/utils/annotations';
import { localeText } from 'src/utils/config';

interface Props {
  /** the taxonomies usable for annotation */
  taxonomies: TaxonomyDto[];
  /** the current attributes of the study */
  attributes: AttributeDto[];
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ add: [annotations: Annotation[]] }>();
const { t, locale } = useI18n();

const filter = ref<string | null>('');
const taxonomyName = ref<string>();
const selected = ref<Annotation[]>([]);

/** the taxonomies with their vocabularies matching the filter: all of them when the taxonomy matches */
const shown = computed(() => {
  const text = filter.value ?? '';
  return props.taxonomies
    .map((taxonomy) => ({
      taxonomy,
      vocabularies: matchesFilter(taxonomy, text, locale.value)
        ? (taxonomy.vocabularies ?? [])
        : (taxonomy.vocabularies ?? []).filter((vocabulary) => matchesFilter(vocabulary, text, locale.value)),
    }))
    .filter((item) => item.vocabularies.length > 0);
});
const current = computed(() => shown.value.find((item) => item.taxonomy.name === taxonomyName.value) ?? shown.value[0]);

function title(entity: TaxonomyDto | TermDto) {
  return localeText(entity.title, locale.value, entity.name);
}

function description(entity: TaxonomyDto | TermDto) {
  return localeText(entity.description, locale.value);
}

function annotated(namespace: string, name: string) {
  return isAnnotated(props.attributes, { namespace, name });
}

function isSelected(namespace: string, name: string) {
  return selected.value.some((item) => item.namespace === namespace && item.name === name);
}

function selectedCount(namespace: string) {
  return selected.value.filter((item) => item.namespace === namespace).length;
}

function toggle(namespace: string, name: string) {
  selected.value = isSelected(namespace, name)
    ? selected.value.filter((item) => item.namespace !== namespace || item.name !== name)
    : [...selected.value, { namespace, name }];
}

function onShow() {
  filter.value = '';
  taxonomyName.value = undefined;
  selected.value = [];
}
</script>
