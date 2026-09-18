import { describe, expect, it } from 'vitest';
import { applyChosenFields, diffSize, fromRestorable, toRestorable } from './restoreFields';

describe('applyChosenFields', () => {
  const entity = {
    name: { en: 'Net', fr: 'Réseau' },
    model: { website: 'https://a.example', nested: { flag: true } },
    studyIds: ['s1', 's2'],
    memberships: [{ role: 'contact', members: [{ email: 'a@x', firstName: 'A' }] }],
  };

  it('sets and removes plain paths on a copy', () => {
    const result = applyChosenFields(entity, [
      { name: 'model.website', value: 'https://b.example' },
      'name.fr',
      { name: 'model.nested.flag', value: false },
      { name: 'model.created.deep', value: 1 },
    ]);
    expect(result.model).toEqual({ website: 'https://b.example', nested: { flag: false }, created: { deep: 1 } });
    expect(result.name).toEqual({ en: 'Net' });
    expect(entity.name.fr).toBe('Réseau');
  });

  it('handles array paths', () => {
    const result = applyChosenFields(entity, [
      { name: 'studyIds[1]', value: 's3' },
      { name: 'studyIds[5]', value: 's4' },
      { name: 'memberships[0].members[0].email', value: 'b@x' },
      { name: 'memberships[1].role', value: 'investigator' },
    ]);
    expect(result.studyIds).toEqual(['s1', 's3', 's4']);
    expect(result.memberships).toEqual([
      { role: 'contact', members: [{ email: 'b@x', firstName: 'A' }] },
      { role: 'investigator' },
    ]);
    const removed = applyChosenFields(result, ['studyIds[0]', 'memberships[0].members[0].firstName', 'memberships[1]']);
    expect(removed.studyIds).toEqual(['s3', 's4']);
    expect(removed.memberships).toEqual([{ role: 'contact', members: [{ email: 'b@x' }] }]);
  });

  it('ignores the removal of a missing path', () => {
    expect(applyChosenFields(entity, ['model.missing.deeper', 'nowhere[2].x'])).toEqual(entity);
  });
});

describe('restorable shape', () => {
  const dto = {
    id: 'net',
    name: [{ lang: 'en', value: 'Net' }],
    acronym: [],
    content: '{"website":"https://a.example"}',
    studyIds: ['s1'],
  };

  it('converts to the diff shape and back', () => {
    const restorable = toRestorable(dto, ['name', 'acronym']);
    expect(restorable).toEqual({ id: 'net', name: { en: 'Net' }, acronym: {}, model: { website: 'https://a.example' }, studyIds: ['s1'] });
    const back = fromRestorable<typeof dto>({ ...restorable, name: { en: 'Renamed' } }, ['name', 'acronym']);
    expect(back).toEqual({ id: 'net', name: [{ lang: 'en', value: 'Renamed' }], acronym: undefined, content: '{"website":"https://a.example"}', studyIds: ['s1'] });
  });

  it('counts the fields of a diff', () => {
    expect(diffSize({ onlyLeft: { a: [] }, differing: { b: [], c: [] }, onlyRight: {} })).toBe(3);
  });
});
