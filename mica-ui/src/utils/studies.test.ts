import { describe, expect, it } from 'vitest';
import type { PopulationDto, PopulationDto_DataCollectionEventDto as DceDto } from 'src/models/Mica';
import { byWeight, cloneEvent, isValidId, moveItem, nextId, studyTimeline } from './studies';

const en = (value: string) => [{ lang: 'en', value }];

function dce(id: string, weight: number, dates: Partial<DceDto>): DceDto {
  return { id, name: en(`DCE ${id}`), description: [], attachments: [], startYear: 2000, weight, ...dates };
}

function population(id: string, weight: number, events: DceDto[]): PopulationDto {
  return { id, name: en(`Population ${id}`), description: [], dataCollectionEvents: events, weight };
}

describe('studies', () => {
  it('generates the next id', () => {
    expect(nextId([])).toBe('1');
    expect(nextId(['1', '2'])).toBe('3');
    expect(nextId(['2', '3', '1'])).toBe('4');
    expect(nextId(['pop9'])).toBe('pop10');
    expect(nextId(['baseline'])).toBe('baseline_1');
  });

  it('validates the ids', () => {
    expect(isValidId('pop_1-a')).toBe(true);
    expect(isValidId('')).toBe(false);
    expect(isValidId('a b')).toBe(false);
    expect(isValidId('a.b')).toBe(false);
    expect(isValidId('a:b')).toBe(false);
  });

  it('orders and moves by weight', () => {
    const items = [
      { id: 'b', weight: 1 },
      { id: 'a', weight: 0 },
      { id: 'c', weight: 2 },
    ];
    expect(byWeight(items).map((item) => item.id)).toEqual(['a', 'b', 'c']);
    expect(moveItem(items, 0, 1)).toEqual([
      { id: 'b', weight: 0 },
      { id: 'a', weight: 1 },
      { id: 'c', weight: 2 },
    ]);
    expect(moveItem(items, 2, 1).map((item) => item.id)).toEqual(['a', 'b', 'c']);
  });

  it('clones an event with the next ids', () => {
    const source = dce('1', 0, { startYear: 2001, endYear: 2002 });
    const events = cloneEvent(population('1', 0, [source, dce('2', 1, {})]), source, 2);
    expect(events.map((event) => [event.id, event.weight, event.endYear])).toEqual([
      ['1', 0, 2002],
      ['2', 1, undefined],
      ['3', 2, 2002],
      ['4', 3, 2002],
    ]);
    expect(events[2]?.name).not.toBe(source.name);
  });

  it('lays out the timeline', () => {
    const today = new Date(2010, 5, 1);
    const timeline = studyTimeline(
      [
        population('2', 1, [dce('1', 0, { startYear: 2005, startMonth: 3, endYear: 2006, endMonth: 2 })]),
        population('1', 0, [
          dce('1', 0, { startYear: 2000, endYear: 2003 }),
          dce('2', 1, { startYear: 2002, startDay: '2002-06-15', endYear: 2004 }),
          dce('3', 2, { startYear: 2004, startMonth: 6 }),
        ]),
        population('3', 2, []),
      ],
      'en',
      today,
    );
    expect(timeline?.min).toEqual(new Date(2000, 0, 1));
    expect(timeline?.max).toEqual(new Date(2011, 0, 1));
    expect(timeline?.populations.map((item) => item.id)).toEqual(['1', '2']);
    const lanes = timeline?.populations[0]?.lanes.map((lane) => lane.map((event) => event.dceId));
    expect(lanes).toEqual([['1', '3'], ['2']]);
    const [first, second] = timeline?.populations[0]?.lanes[0] ?? [];
    expect([first?.start, first?.end, first?.ongoing]).toEqual([new Date(2000, 0, 1), new Date(2003, 11, 31), false]);
    expect([second?.start, second?.end, second?.ongoing]).toEqual([new Date(2004, 5, 1), new Date(2010, 11, 31), true]);
    expect(timeline?.populations[0]?.lanes[1]?.[0]?.start).toEqual(new Date(2002, 5, 15));
    expect(timeline?.populations[1]?.lanes[0]?.[0]?.end).toEqual(new Date(2006, 1, 28));
    expect(studyTimeline([population('1', 0, [])], 'en')).toBeUndefined();
  });
});
