<template>
  <div>
    <q-banner v-if="editor.dirty.value" dense inline-actions class="bg-orange-1 q-mb-sm">
      {{ t('taxonomies.edit.unsaved', { names: editor.dirtyTargets.value.map(micaTaxonomyName).join(', ') }) }}
      <template #action>
        <q-btn
          flat
          :label="t('taxonomies.edit.discard')"
          color="secondary"
          :disable="editor.saving.value"
          @click="onDiscard"
        />
        <q-btn :label="t('save')" color="primary" :loading="editor.saving.value" @click="onSave" />
      </template>
    </q-banner>
    <q-splitter v-model="splitter" :limits="[20, 60]" class="taxonomies-browser">
      <template #before>
        <div class="q-pr-md">
          <q-input v-model="filter" dense outlined clearable :placeholder="t('taxonomies.filter')" class="q-mb-sm">
            <template #prepend>
              <q-icon name="search" />
            </template>
          </q-input>
          <q-tree
            v-model:selected="selectedKey"
            v-model:expanded="expanded"
            :nodes="nodes"
            node-key="key"
            label-key="name"
            :filter="filter || ''"
            :filter-method="filterNode"
            :no-nodes-label="t('taxonomies.none')"
            :no-results-label="t('taxonomies.no_results')"
            selected-color="primary"
            dense
            @lazy-load="onLazyLoad"
          >
            <template #default-header="{ node }">
              <q-icon
                :name="TAXONOMY_NODE_ICONS[node.type as TaxonomyNodeType]"
                size="xs"
                class="q-mr-xs text-grey-7"
              />
              <span>{{ nodeLabel(node) }}</span>
            </template>
          </q-tree>
        </div>
      </template>
      <template #after>
        <div class="q-pl-md">
          <taxonomy-node-panel v-if="selected" :node="selected">
            <template v-if="authStore.isAdministrator" #actions>
              <template v-if="selected.movable">
                <q-btn
                  flat
                  dense
                  round
                  icon="arrow_upward"
                  :disable="moving || !canMove(siblings, selected.key, true)"
                  @click="onMove(true)"
                >
                  <q-tooltip>{{ t('taxonomies.move_up') }}</q-tooltip>
                </q-btn>
                <q-btn
                  flat
                  dense
                  round
                  icon="arrow_downward"
                  :disable="moving || !canMove(siblings, selected.key, false)"
                  @click="onMove(false)"
                >
                  <q-tooltip>{{ t('taxonomies.move_down') }}</q-tooltip>
                </q-btn>
              </template>
              <template v-if="editable">
                <q-btn v-if="canAdd" flat dense round icon="add" @click="onAdd">
                  <q-tooltip>{{
                    t(selected.type === 'taxonomy' ? 'taxonomies.edit.vocabulary_add' : 'taxonomies.edit.term_add')
                  }}</q-tooltip>
                </q-btn>
                <q-btn flat dense round icon="edit" @click="onEdit">
                  <q-tooltip>{{ t('edit') }}</q-tooltip>
                </q-btn>
                <template v-if="selected.type !== 'taxonomy'">
                  <q-btn flat dense round icon="arrow_upward" :disable="position <= 0" @click="onMoveItem(true)">
                    <q-tooltip>{{ t('taxonomies.move_up') }}</q-tooltip>
                  </q-btn>
                  <q-btn
                    flat
                    dense
                    round
                    icon="arrow_downward"
                    :disable="position >= itemSiblings.length - 1"
                    @click="onMoveItem(false)"
                  >
                    <q-tooltip>{{ t('taxonomies.move_down') }}</q-tooltip>
                  </q-btn>
                  <q-btn v-if="!locked" flat dense round icon="delete" color="negative" @click="onDelete">
                    <q-tooltip>{{ t('delete') }}</q-tooltip>
                  </q-btn>
                </template>
              </template>
            </template>
          </taxonomy-node-panel>
          <div v-else class="text-grey-7">{{ t('taxonomies.select_hint') }}</div>
        </div>
      </template>
    </q-splitter>
    <taxonomy-edit-dialog
      v-model="dialog.show"
      :kind="dialog.kind"
      :target="dialog.target"
      :entity="dialog.entity"
      :siblings="dialog.siblings"
      :vocabulary="dialog.vocabulary"
      :fields="fields"
      @save="dialog.apply"
    />
  </div>
</template>

<script setup lang="ts">
import { Notify, useQuasar } from 'quasar';
import { api } from 'src/boot/api';
import TaxonomyEditDialog from 'src/components/settings/taxonomies/TaxonomyEditDialog.vue';
import TaxonomyNodePanel from 'src/components/settings/taxonomies/TaxonomyNodePanel.vue';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import {
  TAXONOMY_NODE_ICONS,
  TAXONOMY_TARGETS,
  ancestorKeys,
  canMove,
  findNode,
  isMicaNode,
  micaTaxonomyName,
  swapNode,
  targetNodes,
  taxonomyNodes,
  useTaxonomies,
  useTaxonomyEditor,
  type TaxonomyNode,
  type TaxonomyNodeType,
  type TaxonomyTarget,
} from 'src/composables/useTaxonomies';
import type { TaxonomyDto, TermDto, VocabularyDto } from 'src/models/Opal';
import { localeText } from 'src/utils/config';
import { notifyError, notifySuccess } from 'src/utils/notify';
import {
  TARGET_FORMS,
  TARGET_INDEX_PATHS,
  isStatic,
  moveItem,
  schemaFields,
  taxonomyError,
} from 'src/utils/taxonomyConfig';

/** the key of the selected node */
const selectedKey = defineModel<string | null>({ default: null });

const { t, locale } = useI18n();
const $q = useQuasar();
const authStore = useAuthStore();
const systemStore = useSystemStore();
const formsStore = useFormsStore();
const { moving, loadTarget, move } = useTaxonomies();
const editor = useTaxonomyEditor();
useConfirmLeave(editor.dirty);

const splitter = ref(25);
const filter = ref<string | null>('');
const expanded = ref<string[]>([]);

/** the targets whose documents are enabled */
const nodes = ref<TaxonomyNode[]>([]);

const selected = computed(() => (selectedKey.value ? findNode(nodes.value, selectedKey.value) : undefined));

/** the nodes among which the selected taxonomy moves */
const siblings = computed(() => (selected.value ? findNode(nodes.value, selected.value.target)?.children || [] : []));

/** the parent of the selected node */
const parent = computed(() =>
  selected.value ? findNode(nodes.value, selected.value.key.split('/').slice(0, -1).join('/')) : undefined,
);

/** the vocabularies and their (first level) terms of the Mica taxonomies are edited */
const editable = computed(
  () => !!selected.value && isMicaNode(selected.value) && selected.value.key.split('/').length <= 4,
);

/** the vocabulary of the selected vocabulary or term */
const selectedVocabulary = computed(() => {
  if (selected.value?.type === 'vocabulary') return selected.value.entity as VocabularyDto;
  if (selected.value?.type === 'term') return parent.value?.entity as VocabularyDto | undefined;
  return undefined;
});

/** the terms of a built-in vocabulary are fixed, as is the vocabulary */
const locked = computed(() => isStatic(selectedVocabulary.value));

const canAdd = computed(
  () => selected.value?.type === 'taxonomy' || (selected.value?.type === 'vocabulary' && !locked.value),
);

const itemSiblings = computed(() => parent.value?.children || []);
const position = computed(() => itemSiblings.value.findIndex((node) => node.key === selected.value?.key));

type Entity = TaxonomyDto | VocabularyDto | TermDto;

const dialog = ref<{
  show: boolean;
  kind: 'taxonomy' | 'vocabulary' | 'term';
  target: TaxonomyTarget;
  entity?: Entity | undefined;
  siblings: Entity[];
  vocabulary?: VocabularyDto | undefined;
  /** puts the dialog result in the working copy */
  apply: (entity: Entity) => void;
}>({ show: false, kind: 'taxonomy', target: 'study', siblings: [], apply: () => undefined });

/** the searchable fields of the documents of the target */
const fields = ref<string[]>([]);

function nodeLabel(node: TaxonomyNode) {
  return node.type === 'target'
    ? t(`taxonomies.targets.${node.name}`)
    : localeText(node.entity?.title, locale.value, node.name);
}

function filterNode(node: TaxonomyNode, text: string) {
  const query = text.toLowerCase();
  return node.name.toLowerCase().includes(query) || nodeLabel(node).toLowerCase().includes(query);
}

function onLazyLoad({
  node,
  done,
  fail,
}: {
  node: TaxonomyNode;
  done: (children: TaxonomyNode[]) => void;
  fail: () => void;
}) {
  loadTarget(node.target)
    .then(done)
    .catch((error) => {
      notifyError(error);
      fail();
    });
}

async function onMove(up: boolean) {
  const node = selected.value;
  if (!node) return;
  if (await move(node.target, node.name, up)) {
    swapNode(siblings.value, node.key, up);
    notifySuccess('taxonomies.moved');
  }
}

function ask(title: string, message: string): Promise<boolean> {
  return new Promise((resolve) => {
    $q.dialog({ title, message, cancel: true, persistent: true })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false));
  });
}

/** shows the working copy of the Mica taxonomy of the target in the tree */
function showDraft(target: TaxonomyTarget) {
  const root = findNode(nodes.value, target);
  const taxonomy = editor.drafts.value[target]?.taxonomy;
  const idx = (root?.children || []).findIndex((node) => node.name === micaTaxonomyName(target));
  if (!root?.children || !taxonomy || idx === -1) return;
  const movable = root.children[idx]!.movable ? [taxonomy.name] : [];
  root.children[idx] = taxonomyNodes(target, [taxonomy], movable)[0]!;
}

function select(key: string) {
  expanded.value = [...new Set([...expanded.value, ...ancestorKeys(key)])];
  selectedKey.value = key;
}

/** the selected taxonomy, vocabulary and term in the working copy */
async function locate(node: TaxonomyNode) {
  const taxonomy = await editor.draft(node.target);
  showDraft(node.target);
  const [, , vocabularyName, termName] = node.key.split('/');
  const vocabulary = vocabularyName ? taxonomy.vocabularies?.find((v) => v.name === vocabularyName) : undefined;
  const term = termName ? vocabulary?.terms?.find((v) => v.name === termName) : undefined;
  if ((vocabularyName && !vocabulary) || (termName && !term)) {
    // the browsed taxonomy holds terms added by the server (the document ids for instance)
    notifyError('taxonomies.edit.not_found');
    return undefined;
  }
  return { taxonomy, vocabulary, term };
}

async function loadFields(target: TaxonomyTarget) {
  const forms = TARGET_FORMS[target] || [];
  const results = await Promise.allSettled(
    forms.map(({ form }) => formsStore.getForm(`/config/${form}/form`, locale.value)),
  );
  const paths = results.flatMap((result, idx) =>
    result.status === 'fulfilled' ? schemaFields(result.value.schema, forms[idx]!.prefix) : [],
  );
  fields.value = [...new Set(paths)].sort();
}

function openDialog(options: Omit<typeof dialog.value, 'show'>) {
  dialog.value = { ...options, show: true };
  if (options.kind === 'vocabulary') void loadFields(options.target);
}

async function onEdit() {
  const node = selected.value;
  const found = node && (await locate(node));
  if (!node || !found) return;
  const { taxonomy, vocabulary, term } = found;
  const base = `${node.target}/${taxonomy.name}`;
  if (node.type === 'taxonomy') {
    openDialog({
      kind: 'taxonomy',
      target: node.target,
      entity: taxonomy,
      siblings: [],
      apply: (entity) => {
        Object.assign(taxonomy, entity);
        showDraft(node.target);
      },
    });
  } else if (node.type === 'vocabulary' && vocabulary) {
    const list = taxonomy.vocabularies!;
    openDialog({
      kind: 'vocabulary',
      target: node.target,
      entity: vocabulary,
      siblings: list,
      apply: (entity) => {
        list.splice(list.indexOf(vocabulary), 1, entity as VocabularyDto);
        showDraft(node.target);
        select(`${base}/${entity.name}`);
      },
    });
  } else if (vocabulary && term) {
    const list = vocabulary.terms!;
    openDialog({
      kind: 'term',
      target: node.target,
      entity: term,
      siblings: list,
      vocabulary,
      apply: (entity) => {
        list.splice(list.indexOf(term), 1, entity as TermDto);
        showDraft(node.target);
        select(`${base}/${vocabulary.name}/${entity.name}`);
      },
    });
  }
}

async function onAdd() {
  const node = selected.value;
  const found = node && (await locate(node));
  if (!node || !found) return;
  const { taxonomy, vocabulary } = found;
  const base = `${node.target}/${taxonomy.name}`;
  if (node.type === 'taxonomy') {
    taxonomy.vocabularies = taxonomy.vocabularies || [];
    const list = taxonomy.vocabularies;
    openDialog({
      kind: 'vocabulary',
      target: node.target,
      siblings: list,
      apply: (entity) => {
        list.push(entity as VocabularyDto);
        showDraft(node.target);
        select(`${base}/${entity.name}`);
      },
    });
  } else if (vocabulary) {
    vocabulary.terms = vocabulary.terms || [];
    const list = vocabulary.terms;
    openDialog({
      kind: 'term',
      target: node.target,
      siblings: list,
      vocabulary,
      apply: (entity) => {
        list.push(entity as TermDto);
        showDraft(node.target);
        select(`${base}/${vocabulary.name}/${entity.name}`);
      },
    });
  }
}

/** the list holding the selected vocabulary or term in the working copy, and its position */
async function locateItem() {
  const node = selected.value;
  const found = node && (await locate(node));
  if (!node || !found) return undefined;
  const list: Entity[] = (node.type === 'term' ? found.vocabulary?.terms : found.taxonomy.vocabularies) || [];
  const item = node.type === 'term' ? found.term : found.vocabulary;
  return { node, list, index: item ? list.indexOf(item) : -1 };
}

async function onMoveItem(up: boolean) {
  const found = await locateItem();
  if (!found || found.index === -1) return;
  moveItem(found.list, found.index, up);
  showDraft(found.node.target);
}

async function onDelete() {
  const node = selected.value;
  if (!node) return;
  const type = t(`taxonomies.types.${node.type}`);
  if (!(await ask(t('taxonomies.edit.delete_title', { type }), t('taxonomies.edit.delete_text', { name: node.name }))))
    return;
  const found = await locateItem();
  if (!found || found.index === -1) return;
  found.list.splice(found.index, 1);
  showDraft(node.target);
  selectedKey.value = node.key.split('/').slice(0, -1).join('/');
}

/** the targets trees reloaded from the server, without the working copies */
async function reload(targets: TaxonomyTarget[]) {
  for (const target of targets) {
    const root = findNode(nodes.value, target);
    if (!root) continue;
    try {
      root.children = await loadTarget(target);
      root.lazy = false;
    } catch (error) {
      notifyError(error);
    }
  }
}

function onDiscard() {
  const targets = Object.keys(editor.drafts.value) as TaxonomyTarget[];
  editor.discard();
  void reload(targets);
}

async function onSave() {
  const targets = [...editor.dirtyTargets.value];
  for (const target of targets) {
    const invalid = taxonomyError(editor.drafts.value[target]!.taxonomy);
    if (invalid) {
      Notify.create({ type: 'negative', message: t(invalid.error, { name: invalid.vocabulary }) });
      return;
    }
  }
  const saved: TaxonomyTarget[] = [];
  try {
    for (const target of targets) {
      const text = t('taxonomies.edit.changed_text', { name: micaTaxonomyName(target) });
      if ((await editor.changedSince(target)) && !(await ask(t('taxonomies.edit.changed_title'), text))) break;
      await editor.save(target);
      saved.push(target);
    }
  } catch (error) {
    notifyError(error);
  }
  if (!saved.length) return;
  // the working copies left unchanged are dropped as well
  const unchanged = (Object.keys(editor.drafts.value) as TaxonomyTarget[]).filter(
    (target) => !editor.dirtyTargets.value.includes(target),
  );
  unchanged.forEach((target) => delete editor.drafts.value[target]);
  await reload([...saved, ...unchanged]);
  notifySuccess('taxonomies.edit.saved');
  if (await ask(t('taxonomies.edit.reindex_title'), t('taxonomies.edit.reindex_text'))) await reindex(saved);
}

/** the mapping of the documents index follows the vocabularies once the index is rebuilt */
async function reindex(targets: TaxonomyTarget[]) {
  try {
    for (const path of new Set(targets.map((target) => TARGET_INDEX_PATHS[target]!))) await api.put(path);
    notifySuccess('taxonomies.edit.reindexing');
  } catch (error) {
    notifyError(error);
  }
}

function enabledTargets(): TaxonomyTarget[] {
  const config = systemStore.configuration;
  const datasets = config.isCollectedDatasetEnabled || config.isHarmonizedDatasetEnabled;
  return TAXONOMY_TARGETS.filter((target) => {
    if (target === 'network') return config.isNetworkEnabled;
    if (target === 'variable' || target === 'dataset') return datasets;
    return true;
  });
}

/** the tree of the enabled targets, the one of the selected node loaded and expanded */
async function init() {
  await systemStore.init();
  nodes.value = targetNodes(enabledTargets());
  const key = selectedKey.value;
  const root = key ? nodes.value.find((node) => node.key === key.split('/')[0]) : undefined;
  if (!key || !root) return;
  try {
    root.children = await loadTarget(root.target);
    root.lazy = false;
    expanded.value = ancestorKeys(key);
  } catch (error) {
    notifyError(error);
  }
}

onMounted(init);
</script>

<style scoped>
/* both panes scroll on their own, the properties stay in view along a long tree */
.taxonomies-browser {
  height: calc(100vh - 230px);
  min-height: 400px;
}
</style>
