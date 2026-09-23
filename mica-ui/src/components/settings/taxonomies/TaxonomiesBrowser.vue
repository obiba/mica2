<template>
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
            <q-icon :name="TAXONOMY_NODE_ICONS[node.type as TaxonomyNodeType]" size="xs" class="q-mr-xs text-grey-7" />
            <span>{{ nodeLabel(node) }}</span>
          </template>
        </q-tree>
      </div>
    </template>
    <template #after>
      <div class="q-pl-md">
        <taxonomy-node-panel v-if="selected" :node="selected">
          <template v-if="selected.movable && authStore.isAdministrator" #actions>
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
        </taxonomy-node-panel>
        <div v-else class="text-grey-7">{{ t('taxonomies.select_hint') }}</div>
      </div>
    </template>
  </q-splitter>
</template>

<script setup lang="ts">
import TaxonomyNodePanel from 'src/components/settings/taxonomies/TaxonomyNodePanel.vue';
import {
  TAXONOMY_NODE_ICONS,
  TAXONOMY_TARGETS,
  ancestorKeys,
  canMove,
  findNode,
  swapNode,
  targetNodes,
  useTaxonomies,
  type TaxonomyNode,
  type TaxonomyNodeType,
  type TaxonomyTarget,
} from 'src/composables/useTaxonomies';
import { localeText } from 'src/utils/config';
import { notifyError, notifySuccess } from 'src/utils/notify';

/** the key of the selected node */
const selectedKey = defineModel<string | null>({ default: null });

const { t, locale } = useI18n();
const authStore = useAuthStore();
const systemStore = useSystemStore();
const { moving, loadTarget, move } = useTaxonomies();

const splitter = ref(25);
const filter = ref<string | null>('');
const expanded = ref<string[]>([]);

/** the targets whose documents are enabled */
const nodes = ref<TaxonomyNode[]>([]);

const selected = computed(() => (selectedKey.value ? findNode(nodes.value, selectedKey.value) : undefined));

/** the nodes among which the selected taxonomy moves */
const siblings = computed(() => (selected.value ? findNode(nodes.value, selected.value.target)?.children || [] : []));

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
