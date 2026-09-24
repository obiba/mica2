import { describe, expect, it } from 'vitest';
import type { NetworkDto } from 'src/models/Mica';
import {
  addLinks,
  associatedPeopleQuery,
  linkedIds,
  networkLinks,
  removeLinks,
} from './networks';

const en = (value: string) => [{ lang: 'en', value }];

const network: NetworkDto = {
  id: 'n1',
  name: en('Network'),
  acronym: en('N1'),
  description: [],
  investigators: [],
  contacts: [],
  attachments: [],
  studyIds: ['s1', 'h1'],
  studySummaries: [
    {
      id: 's1',
      name: en('Study'),
      acronym: en('S1'),
      countries: [],
      objectives: [],
      dataSources: [],
      populationSummaries: [],
      published: true,
      studyResourcePath: 'individual-study',
      permissions: { view: true, add: false, edit: false, delete: false, publish: false },
    },
    {
      id: 'h1',
      name: en('Initiative'),
      acronym: en('H1'),
      countries: [],
      objectives: [],
      dataSources: [],
      populationSummaries: [],
      published: false,
      studyResourcePath: 'harmonization-study',
    },
  ],
  memberships: [],
  networkIds: ['n2'],
  networkSummaries: [
    { id: 'n2', name: en('Other'), acronym: en('N2'), published: true, studyIds: ['s2', 's1'], networkIds: [] },
  ],
  membershipSortOrder: [],
  published: false,
};

describe('networks', () => {
  it('lists the links of a kind, routed when viewable', () => {
    expect(networkLinks(network, 'individual-study', 'en')).toEqual([
      { id: 's1', acronym: 'S1', name: 'Study', published: true, route: '/individual-study/s1' },
    ]);
    expect(networkLinks(network, 'harmonization-study', 'en').map((link) => [link.id, link.route])).toEqual([
      ['h1', undefined],
    ]);
    expect(networkLinks(network, 'network', 'en').map((link) => link.id)).toEqual(['n2']);
  });

  it('adds and removes links', () => {
    expect(addLinks(network, 'individual-study', ['s1', 's3']).studyIds).toEqual(['s1', 'h1', 's3']);
    expect(removeLinks(network, 'harmonization-study', ['h1']).studyIds).toEqual(['s1']);
    expect(addLinks(network, 'network', ['n3']).networkIds).toEqual(['n2', 'n3']);
    expect(linkedIds(network, 'network')).toEqual(['n2', 'n1']);
  });


  it('queries the associated people', () => {
    expect(associatedPeopleQuery(network)).toBe(
      'studyMemberships.parentId:("s1" OR "h1" OR "s2") OR networkMemberships.parentId:("n2")',
    );
    expect(associatedPeopleQuery({ ...network, studyIds: [], networkIds: [], networkSummaries: [] })).toBeUndefined();
  });
});
