import { describe, expect, it } from 'vitest';
import type { DatasetDto_StudyTableDto } from 'src/models/Mica';
import { isSourceComplete, sourceUrn, tableSource, withSource, type TableSource } from './datasets';

describe('tableSource / sourceUrn', () => {
  const cases: [string, TableSource][] = [
    ['urn:opal:CLSA.Tracking', { namespace: 'opal', project: 'CLSA', table: 'Tracking' }],
    ['urn:opal:CLSA.Tracking.v2', { namespace: 'opal', project: 'CLSA', table: 'Tracking.v2' }],
    ['urn:file:/study/s1/dictionary.xlsx', { namespace: 'file', path: '/study/s1/dictionary.xlsx' }],
    ['urn:file:dictionary.xlsx:Baseline', { namespace: 'file', path: 'dictionary.xlsx', table: 'Baseline' }],
    ['urn:plugin:some:thing', { namespace: 'other', nid: 'plugin', nss: 'some:thing' }],
  ];

  it.each(cases)('round trips %s', (urn, source) => {
    expect(tableSource({ source: urn })).toEqual(source);
    expect(sourceUrn(source)).toBe(urn);
  });

  it('reads the legacy Opal fields when there is no source', () => {
    expect(tableSource({ project: 'p', table: 't' })).toEqual({ namespace: 'opal', project: 'p', table: 't' });
    expect(tableSource(undefined)).toEqual({ namespace: 'opal', project: '', table: '' });
  });
});

describe('isSourceComplete', () => {
  it('requires what the namespace needs', () => {
    expect(isSourceComplete({ namespace: 'opal', project: 'p', table: '' })).toBe(false);
    expect(isSourceComplete({ namespace: 'opal', project: 'p', table: 't' })).toBe(true);
    expect(isSourceComplete({ namespace: 'file', path: 'a.xlsx' })).toBe(true);
    expect(isSourceComplete({ namespace: 'other', nid: 'x', nss: '' })).toBe(false);
  });
});

describe('withSource', () => {
  it('drops the legacy Opal fields, which the server would prefer to the source', () => {
    const table: DatasetDto_StudyTableDto = {
      studyId: 's1',
      project: 'p',
      table: 't',
      source: 'urn:opal:p.t',
      name: [],
      description: [],
      additionalInformation: [],
    };
    const updated = withSource(table, { namespace: 'file', path: 'a.xlsx' });
    expect(updated).toEqual({
      studyId: 's1',
      source: 'urn:file:a.xlsx',
      name: [],
      description: [],
      additionalInformation: [],
    });
    expect(table.project).toBe('p');
  });
});
