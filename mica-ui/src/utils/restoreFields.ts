import { localizedToArray, localizedToObject } from 'src/composables/useDocumentModel';
import type { LocalizedStringDto } from 'src/models/Mica';

/**
 * A field chosen in a revision diff to be restored: a path to remove (the field exists in the
 * current document only), or a path and the value of the older revision to set.
 */
export type ChosenField = string | { name: string; value: unknown };

/** the diff of two revisions as served by `_diff`: `[label, value]` or `[label, left, right]` per field path */
export interface DocumentDiff {
  onlyLeft: Record<string, unknown[]>;
  differing: Record<string, unknown[]>;
  onlyRight: Record<string, unknown[]>;
}

type Entity = Record<string, unknown>;

const ARRAY_SEGMENT = /^(\w+)\[(\d+)?]$/;

interface Segment {
  key: string;
  index?: number | undefined;
  array: boolean;
}

function parseSegment(segment: string): Segment {
  const found = segment.match(ARRAY_SEGMENT);
  if (!found) return { key: segment, array: false };
  return { key: found[1] as string, index: found[2] === undefined ? undefined : Number.parseInt(found[2]), array: true };
}

function isObject(value: unknown): value is Entity {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/** descends into (creating when needed) the container of the last segment */
function containerForSet(entity: Entity, segments: Segment[]): Entity {
  let current = entity;
  segments.forEach((segment) => {
    if (!segment.array) {
      if (!isObject(current[segment.key])) current[segment.key] = {};
      current = current[segment.key] as Entity;
      return;
    }
    if (!Array.isArray(current[segment.key])) current[segment.key] = [];
    const list = current[segment.key] as unknown[];
    if (segment.index !== undefined && segment.index < list.length && isObject(list[segment.index])) {
      current = list[segment.index] as Entity;
    } else {
      const item: Entity = {};
      list.push(item);
      current = item;
    }
  });
  return current;
}

function setField(entity: Entity, path: string, value: unknown) {
  const segments = path.split('.').map(parseSegment);
  const last = segments.pop() as Segment;
  const container = containerForSet(entity, segments);
  if (!last.array) {
    container[last.key] = value;
    return;
  }
  const list = Array.isArray(container[last.key]) ? (container[last.key] as unknown[]) : [];
  if (last.index !== undefined && last.index < list.length) {
    list[last.index] = value;
  } else if (!list.includes(value)) {
    list.push(value);
  }
  container[last.key] = list;
}

/** descends without creating; undefined when the path does not exist */
function containerForRemove(entity: Entity, segments: Segment[]): Entity | undefined {
  let current: unknown = entity;
  for (const segment of segments) {
    if (!isObject(current)) return undefined;
    const next = current[segment.key];
    if (segment.array) {
      if (!Array.isArray(next) || segment.index === undefined) return undefined;
      current = next[segment.index];
    } else {
      current = next;
    }
  }
  return isObject(current) ? current : undefined;
}

function removeField(entity: Entity, path: string) {
  const segments = path.split('.').map(parseSegment);
  const last = segments.pop() as Segment;
  const container = containerForRemove(entity, segments);
  if (!container) return;
  if (last.array && Array.isArray(container[last.key]) && last.index !== undefined) {
    (container[last.key] as unknown[]).splice(last.index, 1);
  } else {
    delete container[last.key];
  }
}

/**
 * A copy of the entity with the chosen fields of an older revision applied: paths given as
 * strings are removed, paths with a value are set. Paths are dotted, with `key[index]` for the
 * arrays (`memberships[0].members[1].email`), as the `_diff` service reports them.
 */
export function applyChosenFields<T extends object>(entity: T, chosen: ChosenField[]): T {
  const result = JSON.parse(JSON.stringify(entity)) as Entity;
  chosen.forEach((field) => {
    if (typeof field === 'string') {
      removeField(result, field);
    } else {
      setField(result, field.name, field.value);
    }
  });
  return result as T;
}

/**
 * The shape the diff paths refer to: the localized fields as `{lang: value}` objects and the
 * `content` parsed as `model` (the legacy `deserializeForRestoringFields`).
 */
export function toRestorable(document: object, fields: string[]): Entity {
  const record = document as Entity;
  const result: Entity = { ...record };
  fields.forEach((field) => {
    result[field] = localizedToObject(record[field] as LocalizedStringDto[] | undefined);
  });
  result.model = typeof record.content === 'string' && record.content ? JSON.parse(record.content) : {};
  delete result.content;
  return result;
}

/** back to the DTO shape (the legacy `serializeForRestoringFields`) */
export function fromRestorable<T extends object>(entity: Entity, fields: string[]): T {
  const result: Entity = { ...entity };
  fields.forEach((field) => {
    result[field] = localizedToArray(entity[field]);
  });
  result.content = JSON.stringify(entity.model ?? {});
  delete result.model;
  return result as T;
}

/** the number of fields of a diff, and whether it is empty */
export function diffSize(diff: DocumentDiff): number {
  return Object.keys(diff.onlyLeft).length + Object.keys(diff.differing).length + Object.keys(diff.onlyRight).length;
}
