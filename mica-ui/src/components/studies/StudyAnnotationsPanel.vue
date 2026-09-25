<template>
  <div>
    <div class="text-help q-mb-md">{{ t('annotations.help') }}</div>
    <q-banner v-if="configured.length === 0" rounded class="bg-blue-1 q-mb-md">
      <template #avatar>
        <q-icon name="info" color="primary" />
      </template>
      {{ t('annotations.all_taxonomies') }}
      <router-link to="/settings/general" class="text-primary">{{ t('annotations.configure') }}</router-link>
    </q-banner>
    <div class="q-mb-md">
      <q-btn
        color="primary"
        icon="add"
        :label="t('annotations.add')"
        size="sm"
        :disable="working || taxonomies.length === 0"
        @click="showAdd = true"
      />
    </div>
    <q-spinner-dots v-if="loading" color="primary" size="2em" />
    <div v-else-if="groups.length === 0" class="text-hint">{{ t('annotations.none') }}</div>
    <div v-else class="column q-gutter-md">
      <q-card v-for="group in groups" :key="group.name" flat bordered>
        <q-card-section class="row items-start no-wrap bg-grey-1">
          <div class="col">
            <div class="text-subtitle1 text-weight-medium">
              {{ group.taxonomy ? title(group.taxonomy, locale) : group.name }}
              <q-icon v-if="!group.taxonomy || !group.configured" name="warning" color="warning" size="xs" class="q-ml-xs">
                <q-tooltip>{{ t(group.taxonomy ? 'annotations.not_configured' : 'annotations.unknown') }}</q-tooltip>
              </q-icon>
            </div>
            <div v-if="group.taxonomy && description(group.taxonomy, locale)" class="text-body2 text-grey-8">
              {{ description(group.taxonomy, locale) }}
            </div>
          </div>
          <q-btn
            flat
            dense
            size="sm"
            icon="delete"
            color="negative"
            :title="t('annotations.remove_all')"
            :disable="working"
            @click="toRemove = { namespace: group.name }"
          />
        </q-card-section>
        <q-separator />
        <q-list separator>
          <q-item v-for="item in group.vocabularies" :key="item.name">
            <q-item-section>
              <q-item-label class="text-weight-medium">
                {{ item.vocabulary ? title(item.vocabulary, locale) : item.name }}
                <q-icon v-if="!item.vocabulary" name="warning" color="warning" size="xs" class="q-ml-xs">
                  <q-tooltip>{{ t('annotations.unknown') }}</q-tooltip>
                </q-icon>
              </q-item-label>
              <q-item-label v-if="item.vocabulary && description(item.vocabulary, locale)" class="text-body2 text-grey-8">
                {{ description(item.vocabulary, locale) }}
              </q-item-label>
              <q-item-label caption>
                <code>{{ item.name }}</code>
              </q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-btn
                flat
                dense
                size="sm"
                icon="delete"
                color="negative"
                :title="t('delete')"
                :disable="working"
                @click="toRemove = { namespace: group.name, name: item.name }"
              />
            </q-item-section>
          </q-item>
        </q-list>
      </q-card>
    </div>

    <study-annotations-dialog v-model="showAdd" :taxonomies="taxonomies" :attributes="attributes" @add="onAdd" />
    <confirm-dialog
      :model-value="toRemove !== undefined"
      :title="t('annotations.remove_title')"
      :text="t(toRemove?.name ? 'annotations.remove_text' : 'annotations.remove_all_text', removeLabels)"
      @update:model-value="toRemove = undefined"
      @confirm="onRemove"
    />
  </div>
</template>

<script setup lang="ts">
import { api } from 'src/boot/api';
import type { AttributeDto } from 'src/models/Mica';
import type { TaxonomyDto } from 'src/models/Opal';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import StudyAnnotationsDialog from 'src/components/studies/StudyAnnotationsDialog.vue';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import {
  addAnnotations,
  annotationTaxonomies,
  description,
  groupAnnotations,
  removeAnnotations,
  title,
  toAttributesBody,
  type Annotation,
} from 'src/utils/annotations';
import { notifyError } from 'src/utils/notify';

interface Props {
  target: DocumentTarget;
  /** the attributes of the study, its annotations */
  attributes: AttributeDto[];
}

const props = defineProps<Props>();
/** the annotations were saved */
const emit = defineEmits<{ saved: [] }>();
const { t, locale } = useI18n();
const systemStore = useSystemStore();

const loading = ref(false);
const saving = ref(false);
const working = computed(() => loading.value || saving.value);
const showAdd = ref(false);
const toRemove = ref<{ namespace: string; name?: string }>();
/** all the variable taxonomies, to describe the annotations by the not configured ones as well */
const allTaxonomies = ref<TaxonomyDto[]>([]);
const configured = computed(() => systemStore.configuration.usableVariableTaxonomiesForConceptTagging ?? []);
const taxonomies = computed(() => annotationTaxonomies(allTaxonomies.value, configured.value));
const groups = computed(() => groupAnnotations(props.attributes, allTaxonomies.value, configured.value));
const removeLabels = computed(() => {
  const group = groups.value.find((item) => item.name === toRemove.value?.namespace);
  const vocabulary = group?.vocabularies.find((item) => item.name === toRemove.value?.name);
  return {
    taxonomy: group?.taxonomy ? title(group.taxonomy, locale.value) : toRemove.value?.namespace,
    vocabulary: vocabulary?.vocabulary ? title(vocabulary.vocabulary, locale.value) : toRemove.value?.name,
  };
});

async function save(attributes: AttributeDto[]) {
  saving.value = true;
  try {
    await api.put(`${props.target.path}/attributes`, toAttributesBody(attributes));
    // still saving until the reloaded attributes arrive: an edit of the stale ones would undo this one
    emit('saved');
  } catch (error) {
    notifyError(error);
    saving.value = false;
  }
}

watch(
  () => props.attributes,
  () => (saving.value = false),
);

function onAdd(annotations: Annotation[]) {
  void save(addAnnotations(props.attributes, annotations));
}

function onRemove() {
  if (!toRemove.value) return;
  void save(removeAnnotations(props.attributes, toRemove.value.namespace, toRemove.value.name));
}

onMounted(async () => {
  loading.value = true;
  try {
    // list mode: all the taxonomies, whatever the state of the taxonomies search index
    const response = await api.get<TaxonomyDto[]>('/taxonomies/_filter', {
      params: { target: 'variable', mode: 'list' },
    });
    allTaxonomies.value = response.data || [];
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
});
</script>
