import type { DatasetDto_HarmonizationTableDto, DatasetDto_StudyTableDto } from 'src/models/Mica';

/** where the data dictionary of a dataset table comes from */
export type SourceNamespace = 'opal' | 'file' | 'other';

export const SOURCE_NAMESPACES: SourceNamespace[] = ['opal', 'file', 'other'];

/**
 * The source of a dataset table, split as the legacy admin app edited it: an Opal table
 * (`urn:opal:{project}.{table}`), a file of the Mica file system (`urn:file:{path}[:{table}]`),
 * or any other source a plugin handles (`urn:{nid}:{nss}`).
 */
export interface TableSource {
  namespace: SourceNamespace;
  /** opal */
  project?: string;
  /** opal, file (optional, when the file describes several tables) */
  table?: string;
  /** file */
  path?: string;
  /** other */
  nid?: string;
  nss?: string;
}

export type DatasetTable = DatasetDto_StudyTableDto | DatasetDto_HarmonizationTableDto;

function splitAt(value: string, separator: string): [string, string | undefined] {
  const index = value.indexOf(separator);
  return index < 0 ? [value, undefined] : [value.substring(0, index), value.substring(index + 1)];
}

/** the source of a table, from its `source` URN or, when missing, from the legacy Opal project and table */
export function tableSource(table: Pick<DatasetTable, 'source' | 'project' | 'table'> | undefined): TableSource {
  const source = table?.source;
  if (!source) return { namespace: 'opal', project: table?.project ?? '', table: table?.table ?? '' };
  if (source.startsWith('urn:opal:')) {
    const [project, name] = splitAt(source.substring('urn:opal:'.length), '.');
    return { namespace: 'opal', project, table: name ?? '' };
  }
  if (source.startsWith('urn:file:')) {
    const [path, name] = splitAt(source.substring('urn:file:'.length), ':');
    return { namespace: 'file', path, ...(name ? { table: name } : {}) };
  }
  const [nid, nss] = splitAt(source.replace(/^urn:/, ''), ':');
  return { namespace: 'other', nid, nss: nss ?? '' };
}

/** the `source` URN of a table */
export function sourceUrn(source: TableSource): string {
  switch (source.namespace) {
    case 'opal':
      return `urn:opal:${source.project ?? ''}.${source.table ?? ''}`;
    case 'file':
      return `urn:file:${source.path ?? ''}${source.table ? `:${source.table}` : ''}`;
    default:
      return `urn:${source.nid ?? ''}:${source.nss ?? ''}`;
  }
}

/** whether the source has what the namespace requires */
export function isSourceComplete(source: TableSource): boolean {
  switch (source.namespace) {
    case 'opal':
      return !!source.project && !!source.table;
    case 'file':
      return !!source.path;
    default:
      return !!source.nid && !!source.nss;
  }
}

/** a copy of the table with the source set, the legacy Opal project and table fields being dropped */
export function withSource<T extends DatasetTable>(table: T, source: TableSource): T {
  const updated = { ...table, source: sourceUrn(source) };
  delete updated.project;
  delete updated.table;
  return updated;
}
