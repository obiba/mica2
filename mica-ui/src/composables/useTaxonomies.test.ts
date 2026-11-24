import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import type { MicaConfigDto } from 'src/models/Mica';
import type { TaxonomyDto } from 'src/models/Opal';
import {
  TAXONOMY_TARGETS,
  ancestorKeys,
  canMove,
  findNode,
  hiddenByConfig,
  metaVisibility,
  sortByOrder,
  swapNode,
  taxonomyNodes,
  useTaxonomies,
  variableOrder,
} from './useTaxonomies';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;

const meta: TaxonomyDto = {
  name: 'Mica_taxonomy',
  vocabularies: [
    {
      name: 'variable',
      terms: [
        {
          name: 'Variable_chars',
          terms: [
            { name: 'Mlstr_area' },
            { name: 'Mica_variable' },
            { name: 'Other', attributes: [{ key: 'hidden', value: 'true' }] },
          ],
        },
        {
          name: 'Scales',
          terms: [{ name: 'Mlstr_social', attributes: [{ key: 'hidden', value: 'TRUE' }] }, { name: 'Mlstr_habits' }],
        },
      ],
    },
    { name: 'study', terms: [{ name: 'Mica_study', attributes: [{ key: 'hidden', value: 'false' }] }] },
  ],
};

const variableTaxonomies: TaxonomyDto[] = [
  { name: 'Other' },
  { name: 'Unordered' },
  {
    name: 'Mlstr_area',
    vocabularies: [{ name: 'Lifestyle', terms: [{ name: 'Sleep', terms: [{ name: 'Naps' }] }] }],
  },
  { name: 'Mica_variable' },
];

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useTaxonomies', () => {
  it('reads the variable taxonomies order from the meta-taxonomy', () => {
    expect(variableOrder(meta)).toEqual(['Mlstr_area', 'Mica_variable', 'Other']);
    expect(variableOrder({ name: 'empty' })).toEqual([]);
  });

  it('sorts by the order, the unknown ones last', () => {
    const sorted = sortByOrder(variableTaxonomies, variableOrder(meta)).map((t) => t.name);
    expect(sorted).toEqual(['Mlstr_area', 'Mica_variable', 'Other', 'Unordered']);
  });

  it('builds the nodes down to the nested terms', () => {
    const nodes = taxonomyNodes('variable', variableTaxonomies, ['Mlstr_area']);
    const naps = findNode(nodes, 'variable/Mlstr_area/Lifestyle/Sleep/Naps');
    expect(naps).toMatchObject({ type: 'term', name: 'Naps', target: 'variable' });
    expect(findNode(nodes, 'variable/Mlstr_area/Lifestyle')?.type).toBe('vocabulary');
    expect(findNode(nodes, 'variable/Mlstr_area')?.movable).toBe(true);
    expect(findNode(nodes, 'variable/Other')?.movable).toBe(false);
    expect(findNode(nodes, 'variable/Nope')).toBeUndefined();
  });

  it('lists the ancestor keys', () => {
    expect(ancestorKeys('variable/Mlstr_area/Lifestyle/Sleep')).toEqual([
      'variable',
      'variable/Mlstr_area',
      'variable/Mlstr_area/Lifestyle',
    ]);
    expect(ancestorKeys('variable')).toEqual([]);
  });

  it('moves among the movable siblings only', () => {
    const nodes = taxonomyNodes('variable', sortByOrder(variableTaxonomies, variableOrder(meta)), variableOrder(meta));
    expect(canMove(nodes, 'variable/Mlstr_area', true)).toBe(false);
    expect(canMove(nodes, 'variable/Mlstr_area', false)).toBe(true);
    expect(canMove(nodes, 'variable/Other', false)).toBe(false);
    expect(canMove(nodes, 'variable/Unordered', true)).toBe(false);
    swapNode(nodes, 'variable/Mica_variable', true);
    expect(nodes.map((n) => n.name)).toEqual(['Mica_variable', 'Mlstr_area', 'Other', 'Unordered']);
    swapNode(nodes, 'variable/Mica_variable', true);
    expect(nodes.map((n) => n.name)).toEqual(['Mica_variable', 'Mlstr_area', 'Other', 'Unordered']);
  });

  it('loads the variable taxonomies in the meta-taxonomy order', async () => {
    mocked.get.mockResolvedValueOnce({ data: variableTaxonomies }).mockResolvedValueOnce({ data: meta });
    const nodes = await useTaxonomies().loadTarget('variable');
    expect(mocked.get).toHaveBeenCalledWith('/taxonomies/_filter', { params: { target: 'variable', mode: 'list' } });
    expect(mocked.get).toHaveBeenCalledWith('/meta-taxonomy');
    expect(nodes.map((n) => n.name)).toEqual(['Mlstr_area', 'Mica_variable', 'Other', 'Unordered']);
  });

  it('loads the other targets visibility from the meta-taxonomy, not movable', async () => {
    mocked.get.mockResolvedValueOnce({ data: [{ name: 'Mica_study' }] }).mockResolvedValueOnce({ data: meta });
    const nodes = await useTaxonomies().loadTarget('study');
    expect(mocked.get).toHaveBeenCalledWith('/meta-taxonomy');
    expect(nodes[0]).toMatchObject({ key: 'study/Mica_study', movable: false, hideable: true, hidden: false });
  });

  it('reads the visibility of the taxonomies of all the groups', () => {
    const variable = metaVisibility(meta, 'variable');
    expect([...variable.entries()]).toEqual([
      ['Mlstr_area', false],
      ['Mica_variable', false],
      ['Other', true],
      ['Mlstr_social', true],
      ['Mlstr_habits', false],
    ]);
    expect(metaVisibility(meta, 'network').size).toBe(0);
  });

  it('marks the variable taxonomies hideable when in the meta-taxonomy', async () => {
    const taxonomies = [...variableTaxonomies, { name: 'Mlstr_social' }];
    mocked.get.mockResolvedValueOnce({ data: taxonomies }).mockResolvedValueOnce({ data: meta });
    const nodes = await useTaxonomies().loadTarget('variable');
    expect(findNode(nodes, 'variable/Other')).toMatchObject({ hideable: true, hidden: true });
    expect(findNode(nodes, 'variable/Mlstr_social')).toMatchObject({ hideable: true, hidden: true, movable: false });
    expect(findNode(nodes, 'variable/Mlstr_area')).toMatchObject({ hideable: true, hidden: false });
    expect(findNode(nodes, 'variable/Unordered')).toMatchObject({ hideable: false, hidden: false });
  });

  it('tells the targets whose taxonomies the Mica config hides', () => {
    const all = { isNetworkEnabled: true, isCollectedDatasetEnabled: true, isHarmonizedDatasetEnabled: true };
    const config = (overrides: Partial<MicaConfigDto>) => ({ ...all, ...overrides }) as MicaConfigDto;
    expect(TAXONOMY_TARGETS.filter((target) => hiddenByConfig(target, config({})))).toEqual([]);
    expect(hiddenByConfig('network', config({ isNetworkEnabled: false }))).toBe(true);
    expect(hiddenByConfig('network', config({ isSingleNetworkEnabled: true }))).toBe(true);
    expect(hiddenByConfig('study', config({ isSingleStudyEnabled: true }))).toBe(false);
    expect(hiddenByConfig('study', config({ isSingleStudyEnabled: true, isHarmonizedDatasetEnabled: false }))).toBe(
      true,
    );
    const noDatasets = config({ isCollectedDatasetEnabled: false, isHarmonizedDatasetEnabled: false });
    expect(TAXONOMY_TARGETS.filter((target) => hiddenByConfig(target, noDatasets))).toEqual(['variable', 'dataset']);
  });

  it('hides and shows a taxonomy', async () => {
    mocked.put.mockResolvedValue({ status: 200 });
    const { setHidden } = useTaxonomies();
    expect(await setHidden('variable', 'Mlstr_social', true)).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/meta-taxonomy/variable/Mlstr_social/_attribute', null, {
      params: { name: 'hidden', value: 'true' },
    });
    expect(await setHidden('study', 'Mica_study', false)).toBe(true);
    expect(mocked.put).toHaveBeenLastCalledWith('/meta-taxonomy/study/Mica_study/_attribute', null, {
      params: { name: 'hidden', value: 'false' },
    });
  });

  it('notifies a failed visibility change', async () => {
    mocked.put.mockRejectedValueOnce(new Error('forbidden'));
    expect(await useTaxonomies().setHidden('variable', 'Mlstr_area', true)).toBe(false);
    expect(notifyError).toHaveBeenCalled();
  });

  it('moves a taxonomy', async () => {
    mocked.put.mockResolvedValueOnce({ status: 200 });
    const { moving, move } = useTaxonomies();
    const pending = move('variable', 'Mica_variable', true);
    expect(moving.value).toBe(true);
    expect(await pending).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/meta-taxonomy/variable/Mica_variable/_move', null, {
      params: { dir: 'up' },
    });
    expect(moving.value).toBe(false);
  });

  it('notifies a failed move', async () => {
    mocked.put.mockRejectedValueOnce(new Error('forbidden'));
    expect(await useTaxonomies().move('variable', 'Mica_variable', false)).toBe(false);
    expect(notifyError).toHaveBeenCalled();
  });
});
