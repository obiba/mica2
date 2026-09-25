import { describe, expect, it } from 'vitest';
import { PersonDto_Type, type PersonDto } from 'src/models/Mica';
import {
  checkDuplicates,
  duplicateQuery,
  fieldQuery,
  fromPersonModel,
  groupMemberships,
  membersByRole,
  moveMember,
  searchQuery,
  toPersonModel,
  withMemberships,
} from './persons';

const en = (value: string) => [{ lang: 'en', value }];

function member(id: string, ...roles: string[]): PersonDto {
  const memberships = (parentId: string) =>
    roles.map((role) => ({ role, parentId, parentAcronym: [], parentName: [] }));
  return { id, lastName: id, studyMemberships: memberships('s1'), networkMemberships: memberships('n1') };
}

const person: PersonDto = {
  id: 'p1',
  firstName: 'Jane',
  lastName: 'Doe',
  email: 'jane@example.org',
  institution: {
    name: en('Univ'),
    department: [],
    address: { street: [], city: en('Montreal'), zip: 'H1', country: { iso: 'CAN', name: [] } },
  },
  studyMemberships: [
    {
      role: 'investigator',
      parentId: 's2',
      parentAcronym: en('S2'),
      parentName: en('Study 2'),
      type: PersonDto_Type.STUDY,
    },
    { role: 'contact', parentId: 's2', parentAcronym: en('S2'), parentName: en('Study 2'), type: PersonDto_Type.STUDY },
    { role: 'contact', parentId: 's1', parentAcronym: en('S1'), parentName: en('Study 1'), type: PersonDto_Type.STUDY },
    {
      role: 'contact',
      parentId: 'h1',
      parentAcronym: en('H1'),
      parentName: en('Initiative'),
      type: PersonDto_Type.INITIATIVE,
    },
  ],
  networkMemberships: [{ role: 'contact', parentId: 'n1', parentAcronym: en('N1'), parentName: en('Network') }],
};

describe('persons', () => {
  it('converts a person to a form model and back', () => {
    const model = toPersonModel(person);
    expect(model).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@example.org',
      institution: {
        name: { en: 'Univ' },
        department: {},
        address: { street: {}, city: { en: 'Montreal' }, zip: 'H1', country: 'CAN' },
      },
    });
    expect(fromPersonModel(person, model)).toEqual({
      ...person,
      institution: {
        name: en('Univ'),
        department: [],
        address: { street: [], city: en('Montreal'), zip: 'H1', country: { iso: 'CAN', name: [] } },
      },
    });
  });

  it('drops the emptied fields', () => {
    const updated = fromPersonModel(person, { lastName: ' Smith ', email: '', institution: { name: {}, address: {} } });
    expect(updated.lastName).toBe('Smith');
    expect(updated.firstName).toBeUndefined();
    expect(updated.email).toBeUndefined();
    expect(updated.institution).toBeUndefined();
    expect(updated.studyMemberships).toBe(person.studyMemberships);
  });

  it('groups the memberships by entity', () => {
    expect(groupMemberships(person, 'study', 'fr')).toEqual([
      { id: 's1', acronym: 'S1', name: 'Study 1', roles: ['contact'], route: '/individual-study/s1' },
      { id: 's2', acronym: 'S2', name: 'Study 2', roles: ['contact', 'investigator'], route: '/individual-study/s2' },
    ]);
    expect(groupMemberships(person, 'initiative', 'en').map((entity) => entity.route)).toEqual([
      '/harmonization-study/h1',
    ]);
    expect(groupMemberships(person, 'network', 'en').map((entity) => entity.id)).toEqual(['n1']);
  });

  it('replaces the roles of the given entities', () => {
    const parent = { id: 's2', acronym: en('S2'), name: en('Study 2') };
    const updated = withMemberships(person, 'study', [parent], ['investigator']);
    expect(groupMemberships(updated, 'study', 'en').find((entity) => entity.id === 's2')?.roles).toEqual([
      'investigator',
    ]);
    expect(groupMemberships(updated, 'initiative', 'en')).toHaveLength(1);
    const removed = withMemberships(person, 'network', [{ id: 'n1', acronym: [], name: [] }], []);
    expect(removed.networkMemberships).toEqual([]);
    const added = withMemberships(person, 'initiative', [{ id: 'h2', acronym: en('H2'), name: [] }], ['contact']);
    expect(added.studyMemberships.at(-1)).toMatchObject({ parentId: 'h2', type: PersonDto_Type.INITIATIVE });
  });

  it('checks the duplicates', () => {
    expect(duplicateQuery({ ...person, firstName: 'Mary Ann' })).toBe('firstName:Mary\\ Ann AND lastName:Doe');
    expect(duplicateQuery({ lastName: '', studyMemberships: [], networkMemberships: [] })).toBe('');
    const others = {
      total: 2,
      from: 0,
      limit: 10,
      persons: [person, { ...person, id: 'p2', email: 'JANE@example.org ' }],
    };
    expect(checkDuplicates(person, others)).toEqual({ sameEmail: 'jane@example.org', sameName: 'Jane Doe' });
    expect(checkDuplicates(person, { ...others, persons: [person] })).toEqual({
      sameEmail: undefined,
      sameName: undefined,
    });
  });

  it('builds the search query of a free text', () => {
    expect(searchQuery('  ')).toBeUndefined();
    expect(searchQuery('jan do')).toBe('jan* do*');
    expect(searchQuery('a:b')).toBe('a\\:b*');
    expect(searchQuery('"jane doe"')).toBe('"jane doe"');
  });

  it('restricts the search query to a field', () => {
    expect(fieldQuery('jan* do*', 'all', false, 'en')).toBe('jan* do*');
    expect(fieldQuery('jan* do*', 'id', false, 'en')).toBe('id:jan* id:do*');
    expect(fieldQuery('jan*', 'name', true, 'fr')).toBe('name.fr.analyzed:jan*');
    expect(fieldQuery('"jane doe"', 'acronym', true, 'en')).toBe('acronym.en.analyzed:"jane doe"');
  });

  it('groups the members by role in the sort order', () => {
    const persons = [member('a', 'contact'), member('b', 'contact', 'investigator'), member('c', 'other')];
    const members = membersByRole(persons, 'network', 'n1', ['investigator', 'contact'], [{ role: 'contact', personIds: ['b'] }]);
    expect(members.map((item) => [item.role, item.persons.map((person) => person.id)])).toEqual([
      ['investigator', ['b']],
      ['contact', ['b', 'a']],
      ['other', ['c']],
    ]);
    expect(moveMember(members, 'contact', 'a', -1)).toEqual([
      { role: 'investigator', personIds: ['b'] },
      { role: 'contact', personIds: ['a', 'b'] },
      { role: 'other', personIds: ['c'] },
    ]);
    expect(membersByRole(persons, 'study', 's1', [], undefined).map((item) => item.role)).toEqual([
      'contact',
      'investigator',
      'other',
    ]);
    expect(membersByRole(persons, 'study', 'n1', [], undefined)).toEqual([]);
    expect(moveMember(members, 'contact', 'a', 1)[1]).toEqual({ role: 'contact', personIds: ['b', 'a'] });
  });
});
