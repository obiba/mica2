import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import type { TaxonomyDto } from 'src/models/Opal';
import {
  ancestorKeys,
  canMove,
  findNode,
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
        { name: 'Variable_chars', terms: [{ name: 'Mlstr_area' }, { name: 'Mica_variable' }, { name: 'Other' }] },
      ],
    },
    { name: 'study', terms: [{ name: 'Mica_study' }] },
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

  it('loads the other targets without the meta-taxonomy', async () => {
    mocked.get.mockResolvedValueOnce({ data: [{ name: 'Mica_study' }] });
    const nodes = await useTaxonomies().loadTarget('study');
    expect(mocked.get).toHaveBeenCalledTimes(1);
    expect(nodes[0]).toMatchObject({ key: 'study/Mica_study', movable: false });
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
