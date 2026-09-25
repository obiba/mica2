import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import type { StudySummaryDto } from 'src/models/Mica';
import {
  canImport,
  differentSections,
  fetchDifferences,
  fetchRemoteStudies,
  importOperations,
  importStatusMessage,
  isImportable,
  parseDifferences,
  saveStudies,
} from './useStudiesImport';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

const mocked = api as unknown as Record<'get' | 'put', ReturnType<typeof vi.fn>>;

const connection = { url: 'https://mica.example.org', username: 'bob', password: 'secret' };

function key(formSection: string, parentFormSection: string) {
  return JSON.stringify({ formSection, parentFormSection, endpoint: '/ws/config/x/form-custom' });
}

function study(id: string): StudySummaryDto {
  return { id } as StudySummaryDto;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('parseDifferences', () => {
  it('propagates the parent sections', () => {
    const diffs = parseDifferences({
      [key('individual-study', 'none')]: true,
      [key('study-population', 'individual-study')]: false,
      [key('data-collection-event', 'study-population')]: true,
    });
    expect(diffs).toEqual([
      { section: 'individual-study', equal: true, importable: true, depth: 0 },
      { section: 'study-population', equal: false, importable: false, depth: 1 },
      { section: 'data-collection-event', equal: true, importable: false, depth: 2 },
    ]);
    expect(canImport(diffs)).toBe(true);
    expect(differentSections(diffs)).toEqual(['study-population']);
    expect(isImportable(diffs, 'data-collection-event')).toBe(false);
  });

  it('cannot import when the study form differs', () => {
    expect(canImport(parseDifferences({ [key('harmonization-study', 'none')]: false }))).toBe(false);
    expect(canImport([])).toBe(false);
  });
});

describe('importOperations', () => {
  it('splits the selection into create, replace and conflicts', () => {
    const operations = importOperations([study('a'), study('b'), study('c')], {
      b: '{"conflict":false,"localPopulationSize":2,"localDCEsSize":3}',
      c: '{"conflict":true,"localPopulationSize":0,"localDCEsSize":0}',
    });
    expect(operations.toCreate.map((s) => s.id)).toEqual(['a']);
    expect(operations.toReplace).toEqual([{ id: 'b', conflict: false, localPopulationSize: 2, localDCEsSize: 3 }]);
    expect(operations.conflicts.map((s) => s.id)).toEqual(['c']);
  });
});

describe('remote calls', () => {
  it('sends the credentials in headers and throws the numeric bodies as problem messages', async () => {
    mocked.get.mockResolvedValue({ data: 401 });
    await expect(fetchDifferences(connection, 'individual-study')).rejects.toBe('studies_import.problems.problem_401');
    expect(mocked.get).toHaveBeenCalledWith('/draft/studies/import/_differences', {
      headers: { 'X-Mica-Remote-Username': 'bob', 'X-Mica-Remote-Password': 'secret' },
      params: { url: connection.url, type: 'individual-study' },
    });
  });

  it('returns the remote studies', async () => {
    mocked.get.mockResolvedValue({ data: [study('a')] });
    await expect(fetchRemoteStudies(connection, 'harmonization-study')).resolves.toEqual([study('a')]);
  });

  it('saves with repeated ids and sections', async () => {
    mocked.put.mockResolvedValue({ data: { a: 200 } });
    await expect(saveStudies(connection, 'individual-study', ['a'], ['study-population'])).resolves.toEqual({ a: 200 });
    expect(mocked.put).toHaveBeenCalledWith('/draft/studies/import/_save', null, {
      headers: { 'X-Mica-Remote-Username': 'bob', 'X-Mica-Remote-Password': 'secret' },
      params: { url: connection.url, type: 'individual-study', ids: ['a'], listDiffsForm: ['study-population'] },
      paramsSerializer: { indexes: null },
    });
  });
});

describe('importStatusMessage', () => {
  it('maps the statuses', () => {
    expect(importStatusMessage(200)).toBe('studies_import.status_ok');
    expect(importStatusMessage(503)).toBe('studies_import.problems.problem_503');
    expect(importStatusMessage(418)).toBe('studies_import.problems.problem_500');
  });
});
