import { api } from 'src/boot/api';
import type { TaxonomyDto, TermDto, VocabularyDto } from 'src/models/Opal';
import { notifyError } from 'src/utils/notify';

/** the targets of the taxonomies, in the search order */
export const TAXONOMY_TARGETS = ['variable', 'dataset', 'study', 'network'] as const;

export type TaxonomyTarget = (typeof TAXONOMY_TARGETS)[number];

export type TaxonomyNodeType = 'target' | 'taxonomy' | 'vocabulary' | 'term';

export const TAXONOMY_NODE_ICONS: Record<TaxonomyNodeType, string> = {
  target: 'category',
  taxonomy: 'account_tree',
  vocabulary: 'list',
  term: 'label',
};

/** a node of the taxonomies tree, its key is the path of names from the target: `variable/Mica_variable/sets` */
export interface TaxonomyNode {
  key: string;
  name: string;
  type: TaxonomyNodeType;
  target: TaxonomyTarget;
  entity?: TaxonomyDto | VocabularyDto | TermDto;
  children?: TaxonomyNode[];
  /** the target nodes load their taxonomies when expanded */
  lazy?: boolean;
  /** a taxonomy whose position can be changed in the meta-taxonomy */
  movable?: boolean;
}

export function targetNodes(targets: readonly TaxonomyTarget[]): TaxonomyNode[] {
  return targets.map((target) => ({ key: target, name: target, type: 'target', target, lazy: true }));
}

function termNodes(parent: string, target: TaxonomyTarget, terms: TermDto[] | undefined): TaxonomyNode[] {
  return (terms || []).map((term) => {
    const key = `${parent}/${term.name}`;
    return { key, name: term.name, type: 'term', target, entity: term, children: termNodes(key, target, term.terms) };
  });
}

/** the taxonomy nodes of a target, with their vocabularies and (nested) terms */
export function taxonomyNodes(
  target: TaxonomyTarget,
  taxonomies: TaxonomyDto[],
  movable: string[] = [],
): TaxonomyNode[] {
  return taxonomies.map((taxonomy) => {
    const key = `${target}/${taxonomy.name}`;
    return {
      key,
      name: taxonomy.name,
      type: 'taxonomy',
      target,
      entity: taxonomy,
      movable: movable.includes(taxonomy.name),
      children: (taxonomy.vocabularies || []).map((vocabulary) => {
        const vocabularyKey = `${key}/${vocabulary.name}`;
        return {
          key: vocabularyKey,
          name: vocabulary.name,
          type: 'vocabulary',
          target,
          entity: vocabulary,
          children: termNodes(vocabularyKey, target, vocabulary.terms),
        };
      }),
    };
  });
}

/** the variable taxonomy names, in the order of the meta-taxonomy (`variable` › `Variable_chars`) */
export function variableOrder(meta: TaxonomyDto): string[] {
  const vocabulary = meta.vocabularies?.find((v) => v.name === 'variable');
  const chars = vocabulary?.terms?.find((term) => term.name === 'Variable_chars');
  return (chars?.terms || []).map((term) => term.name);
}

/** the taxonomies sorted by the order, the ones not in it last, in their original order */
export function sortByOrder(taxonomies: TaxonomyDto[], order: string[]): TaxonomyDto[] {
  const rank = (name: string) => {
    const idx = order.indexOf(name);
    return idx === -1 ? order.length : idx;
  };
  return [...taxonomies].sort((a, b) => rank(a.name) - rank(b.name));
}

export function findNode(nodes: TaxonomyNode[], key: string): TaxonomyNode | undefined {
  for (const node of nodes) {
    if (node.key === key) return node;
    if (key.startsWith(`${node.key}/`)) return findNode(node.children || [], key);
  }
  return undefined;
}

/** the keys of the ancestors of a node, the target first */
export function ancestorKeys(key: string): string[] {
  const names = key.split('/');
  return names.slice(0, -1).map((_, idx) => names.slice(0, idx + 1).join('/'));
}

/** whether the sibling in that direction exists and is movable as well */
export function canMove(siblings: TaxonomyNode[], key: string, up: boolean): boolean {
  const idx = siblings.findIndex((node) => node.key === key);
  const other = siblings[up ? idx - 1 : idx + 1];
  return idx !== -1 && siblings[idx]?.movable === true && other?.movable === true;
}

/** swaps the node with its sibling in that direction */
export function swapNode(siblings: TaxonomyNode[], key: string, up: boolean) {
  const idx = siblings.findIndex((node) => node.key === key);
  const other = up ? idx - 1 : idx + 1;
  if (idx === -1 || other < 0 || other >= siblings.length) return;
  [siblings[idx], siblings[other]] = [siblings[other]!, siblings[idx]!];
}

/**
 * The taxonomies of each target and the order of the variable taxonomies, stored in the
 * meta-taxonomy, that an administrator can change.
 */
export function useTaxonomies() {
  const moving = ref(false);

  /** the taxonomy nodes of a target, the variable ones in the meta-taxonomy order */
  async function loadTarget(target: TaxonomyTarget): Promise<TaxonomyNode[]> {
    const response = await api.get<TaxonomyDto[]>('/taxonomies/_filter', { params: { target, mode: 'list' } });
    const taxonomies = response.data || [];
    if (target !== 'variable') return taxonomyNodes(target, taxonomies);
    const meta = await api.get<TaxonomyDto>('/meta-taxonomy');
    const order = variableOrder(meta.data);
    return taxonomyNodes(target, sortByOrder(taxonomies, order), order);
  }

  async function move(target: TaxonomyTarget, taxonomy: string, up: boolean): Promise<boolean> {
    moving.value = true;
    try {
      await api.put(`/meta-taxonomy/${target}/${taxonomy}/_move`, null, { params: { dir: up ? 'up' : 'down' } });
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      moving.value = false;
    }
  }

  return { moving, loadTarget, move };
}

/** the taxonomy of the Mica documents of a target, the one an administrator can edit: `Mica_study` */
export function micaTaxonomyName(target: TaxonomyTarget): string {
  return `Mica_${target}`;
}

/** whether the node belongs to the Mica taxonomy of its target */
export function isMicaNode(node: TaxonomyNode): boolean {
  return node.type !== 'target' && node.key.split('/')[1] === micaTaxonomyName(node.target);
}

/** the configuration of a Mica taxonomy, as edited and saved */
function configPath(target: TaxonomyTarget) {
  return `/config/${target}/taxonomy`;
}

interface TaxonomyDraft {
  taxonomy: TaxonomyDto;
  /** the configuration as loaded, to detect the changes made since by someone else */
  loaded: string;
}

/**
 * The working copies of the Mica taxonomies an administrator edits: changes are made on the copy
 * and saved at once, the whole taxonomy replacing the stored one.
 */
export function useTaxonomyEditor() {
  const drafts = ref<Partial<Record<TaxonomyTarget, TaxonomyDraft>>>({});
  const saving = ref(false);

  const dirtyTargets = computed(() =>
    (Object.keys(drafts.value) as TaxonomyTarget[]).filter((target) => {
      const draft = drafts.value[target]!;
      return JSON.stringify(draft.taxonomy) !== draft.loaded;
    }),
  );

  const dirty = computed(() => dirtyTargets.value.length > 0);

  /** the working copy of the Mica taxonomy of the target, loaded on the first edit */
  async function draft(target: TaxonomyTarget): Promise<TaxonomyDto> {
    const current = drafts.value[target];
    if (current) return current.taxonomy;
    const response = await api.get<TaxonomyDto>(configPath(target));
    const loaded = JSON.stringify(response.data);
    drafts.value[target] = { taxonomy: JSON.parse(loaded), loaded };
    return drafts.value[target]!.taxonomy;
  }

  /** whether the stored taxonomy changed since its working copy was loaded */
  async function changedSince(target: TaxonomyTarget): Promise<boolean> {
    const response = await api.get<TaxonomyDto>(configPath(target));
    return JSON.stringify(response.data) !== drafts.value[target]?.loaded;
  }

  /** saves the working copy of the target, then forgets it */
  async function save(target: TaxonomyTarget) {
    saving.value = true;
    try {
      await api.put(configPath(target), drafts.value[target]!.taxonomy);
      delete drafts.value[target];
    } finally {
      saving.value = false;
    }
  }

  function discard() {
    drafts.value = {};
  }

  return { drafts, dirty, dirtyTargets, saving, draft, changedSince, save, discard };
}
