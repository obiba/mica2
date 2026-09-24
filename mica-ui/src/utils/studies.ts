import type { PopulationDto, PopulationDto_DataCollectionEventDto as DceDto } from 'src/models/Mica';
import { localized } from 'src/utils/persons';

/** the characters an id of a population or of a data collection event cannot have (legacy `ERRONEOUS_ID_CHARS`) */
const INVALID_ID_CHARS = /[;,\\/?:@&%=+$.!`~*'( ){}[\]<>^#]/;

export function isValidId(id: string): boolean {
  return id !== '' && !INVALID_ID_CHARS.test(id);
}

/** a copy of the items ordered by weight, the ones without weight first (stable) */
export function byWeight<T extends { weight?: number | undefined }>(items: T[] | undefined): T[] {
  return [...(items ?? [])].sort((a, b) => (a.weight ?? 0) - (b.weight ?? 0));
}

/** the next id after the last one: its trailing number incremented (else `_1` appended), not already used */
export function nextId(ids: string[]): string {
  let id = ids[ids.length - 1] ?? '0';
  do {
    const match = /[0-9]+$/.exec(id);
    id = match ? id.slice(0, match.index) + (parseInt(match[0], 10) + 1) : `${id}_1`;
  } while (ids.includes(id));
  return id;
}

/** the items ordered by weight with the one at index moved by delta, the weights renumbered */
export function moveItem<T extends { weight?: number | undefined }>(items: T[], index: number, delta: number): T[] {
  const ordered = byWeight(items);
  const to = index + delta;
  if (index >= 0 && index < ordered.length && to >= 0 && to < ordered.length) {
    ordered.splice(to, 0, ...ordered.splice(index, 1));
  }
  return ordered.map((item, weight) => ({ ...item, weight }));
}

/** the population's events with count copies of one added after the last, with the next ids */
export function cloneEvent(population: PopulationDto, dce: DceDto, count: number): DceDto[] {
  const events = byWeight(population.dataCollectionEvents);
  const ids = events.map((event) => event.id ?? '');
  const last = events.length === 0 ? -1 : (events[events.length - 1]?.weight ?? events.length - 1);
  for (let i = 1; i <= count; i++) {
    const id = nextId(ids);
    ids.push(id);
    events.push({ ...(JSON.parse(JSON.stringify(dce)) as DceDto), id, weight: last + i });
  }
  return events;
}

function parseDay(day: string | undefined): Date | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(day ?? '');
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : undefined;
}

export interface TimelineEvent {
  populationId: string;
  dceId: string;
  label: string;
  start: Date;
  end: Date;
  /** no end year: the event runs up to the current year */
  ongoing: boolean;
}

export interface TimelinePopulation {
  id: string;
  label: string;
  /** the events that do not overlap share a lane */
  lanes: TimelineEvent[][];
}

export interface Timeline {
  /** first day of the first year */
  min: Date;
  /** first day of the year after the last one */
  max: Date;
  populations: TimelinePopulation[];
}

/**
 * The timeline of the populations' data collection events (legacy `mica-study-timeline`): the day
 * when set, else the start of the start month (or year) and the end of the end month (or year); the
 * events without end year end with the current year. Undefined when there is no event.
 */
export function studyTimeline(populations: PopulationDto[], lang: string, today = new Date()): Timeline | undefined {
  const currentYear = today.getFullYear();
  const result = byWeight(populations).map((population) => {
    const lanes: TimelineEvent[][] = [];
    byWeight(population.dataCollectionEvents)
      .filter((dce) => dce.startYear)
      .forEach((dce) => {
        const endYear = dce.endYear ?? Math.max(currentYear, dce.startYear);
        const event: TimelineEvent = {
          populationId: population.id ?? '',
          dceId: dce.id ?? '',
          label: localized(dce.name, lang),
          start: parseDay(dce.startDay) ?? new Date(dce.startYear, (dce.startMonth ?? 1) - 1, 1),
          end: parseDay(dce.endDay) ?? new Date(endYear, dce.endMonth ?? 12, 0),
          ongoing: !dce.endYear,
        };
        const lane = lanes.find((events) => (events.at(-1)?.end ?? event.start) < event.start);
        if (lane) lane.push(event);
        else lanes.push([event]);
      });
    return { id: population.id ?? '', label: localized(population.name, lang), lanes };
  });
  const events = result.flatMap((population) => population.lanes.flat());
  if (events.length === 0) return undefined;
  const minYear = Math.min(...events.map((event) => event.start.getFullYear()));
  const maxYear = Math.max(...events.map((event) => event.end.getFullYear()));
  return {
    min: new Date(minYear, 0, 1),
    max: new Date(maxYear + 1, 0, 1),
    populations: result.filter((population) => population.lanes.length > 0),
  };
}
