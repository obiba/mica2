import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import type { MicaMetricsDto } from 'src/models/Mica';
import {
  documentsListRoute,
  normalizeMetrics,
  portalDataAccessesUrl,
  portalSearchUrl,
  useContentMetrics,
} from './useContentMetrics';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  toPortalUrl: (path: string) => `../mica${path}`,
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;

function properties(counts: Record<string, number>) {
  return Object.entries(counts).map(([name, value]) => ({ name, value }));
}

const dto: MicaMetricsDto = {
  documents: [
    {
      type: 'Network',
      properties: properties({ total: 3, published: 2, under_review: 0, in_edition: 1, to_delete: 0, indexed: 1 }),
    },
    { type: 'DatasetVariable', properties: properties({ published: 100, harmonized: 40 }) },
    {
      type: 'Study',
      properties: properties({ total: 5, published: 4, under_review: 1, in_edition: 0, to_delete: 1, indexed: 4 }),
    },
    { type: 'Unknown', properties: [] },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('normalizeMetrics', () => {
  it('orders the types, maps the counts and derives the documents requiring indexing', () => {
    const metrics = normalizeMetrics(dto);
    expect(metrics.types.map((m) => m.type)).toEqual(['Study', 'Network', 'DatasetVariable']);
    expect(metrics.types[1]?.counts).toEqual({
      total: 3,
      published: 2,
      under_review: 0,
      in_edition: 1,
      to_delete: 0,
      indexed: 1,
    });
    expect(metrics.types[1]?.notIndexed).toBe(1);
    expect(metrics.types[0]?.notIndexed).toBe(0);
    expect(metrics.types[2]?.notIndexed).toBeUndefined();
    expect(metrics.notIndexed).toBe(1);
  });

  it('is empty without documents', () => {
    expect(normalizeMetrics(undefined)).toEqual({ types: [], notIndexed: 0 });
    expect(normalizeMetrics({ documents: [] })).toEqual({ types: [], notIndexed: 0 });
  });
});

describe('links', () => {
  it('routes to the filtered documents lists of this app', () => {
    expect(documentsListRoute('Study', 'PUBLISHED')).toBe('/individual-studies?status=PUBLISHED');
    expect(documentsListRoute('HarmonizationDataset')).toBe('/harmonized-datasets');
    expect(documentsListRoute('DataAccessRequest')).toBeUndefined();
  });

  it('links to the portal search', () => {
    expect(portalSearchUrl('Study', 'published')).toBe(
      '../mica/search#lists?type=studies&query=study(in(Mica_study.className,Study))&display=list',
    );
    expect(portalSearchUrl('Study', 'totalWithVariable')).toBe(
      '../mica/search#lists?type=studies&query=variable(in(Mica_variable.variableType,(Collected))),study(in(Mica_study.className,Study))&display=list',
    );
    expect(portalSearchUrl('HarmonizationStudy', 'variables')).toBe(
      '../mica/search#lists?type=variables&query=study(in(Mica_study.className,HarmonizationStudy))&display=list',
    );
    expect(portalSearchUrl('Network', 'published')).toBe('../mica/search#lists?type=networks&display=list');
    expect(portalSearchUrl('Project', 'published')).toBeUndefined();
    expect(portalDataAccessesUrl()).toBe('../mica/data-accesses');
  });
});

describe('useContentMetrics', () => {
  it('loads the metrics and stamps the refresh', async () => {
    mocked.get.mockResolvedValueOnce({ data: dto });
    const { metrics, refreshedAt, loading, load } = useContentMetrics();
    await load();
    expect(mocked.get).toHaveBeenCalledWith('/config/metrics');
    expect(metrics.value?.types).toHaveLength(3);
    expect(refreshedAt.value).toBeInstanceOf(Date);
    expect(loading.value).toBe(false);
  });

  it('flags a failed load and keeps the previous metrics', async () => {
    mocked.get.mockResolvedValueOnce({ data: dto });
    mocked.get.mockRejectedValueOnce(new Error('500'));
    const { metrics, failed, load } = useContentMetrics();
    await load();
    expect(failed.value).toBe(false);
    await load();
    expect(failed.value).toBe(true);
    expect(metrics.value?.types).toHaveLength(3);
  });

  it('lists the documents requiring indexing, sorted by id', async () => {
    mocked.get.mockResolvedValueOnce({
      data: {
        requireIndexing: [
          { id: 'b', title: [] },
          { id: 'a', title: [] },
        ],
      },
    });
    const { loadIndexHealth } = useContentMetrics();
    const items = await loadIndexHealth('StudyDataset');
    expect(mocked.get).toHaveBeenCalledWith('/collected-datasets/index/health');
    expect(items.map((i) => i.id)).toEqual(['a', 'b']);
  });

  it('has nothing to index for the types without an index health', async () => {
    const { loadIndexHealth } = useContentMetrics();
    expect(await loadIndexHealth('DatasetVariable')).toEqual([]);
    expect(mocked.get).not.toHaveBeenCalled();
  });

  it('indexes the selected documents then reloads the metrics', async () => {
    mocked.put.mockResolvedValueOnce({ status: 204 });
    mocked.get.mockResolvedValueOnce({ data: dto });
    const { index } = useContentMetrics();
    expect(await index('Network', ['n1', 'n2'])).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/draft/networks/_index', null, {
      params: { id: ['n1', 'n2'] },
      paramsSerializer: { indexes: null },
    });
    expect(mocked.get).toHaveBeenCalledWith('/config/metrics');
  });

  it('does not index without a selection', async () => {
    const { index } = useContentMetrics();
    expect(await index('Network', [])).toBe(false);
    expect(mocked.put).not.toHaveBeenCalled();
  });
});
