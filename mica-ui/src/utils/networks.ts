import type { MembershipSortOrderDto, NetworkDto, PersonDto } from 'src/models/Mica';
import { localized } from 'src/utils/persons';

/** the kinds of entities a network links to */
export type NetworkLinkKind = 'individual-study' | 'harmonization-study' | 'network';

/** an entity linked to a network, as listed in the network pages */
export interface NetworkLink {
  id: string;
  acronym: string;
  name: string;
  published: boolean;
  /** the route of the entity, undefined when it cannot be viewed */
  route: string | undefined;
}

function linkField(kind: NetworkLinkKind): 'studyIds' | 'networkIds' {
  return kind === 'network' ? 'networkIds' : 'studyIds';
}

/** the linked entities of a kind, ordered by acronym */
export function networkLinks(network: NetworkDto, kind: NetworkLinkKind, lang: string): NetworkLink[] {
  const summaries =
    kind === 'network'
      ? (network.networkSummaries ?? [])
      : (network.studySummaries ?? []).filter((summary) => (summary.studyResourcePath ?? 'individual-study') === kind);
  return summaries
    .filter((summary) => summary.id)
    .map((summary) => ({
      id: summary.id as string,
      acronym: localized(summary.acronym, lang),
      name: localized(summary.name, lang),
      published: summary.published,
      route: summary.permissions?.view ? `/${kind}/${summary.id}` : undefined,
    }))
    .sort((a, b) => a.acronym.localeCompare(b.acronym));
}

/** the ids of the entities of a kind already linked, the network itself being one for the networks */
export function linkedIds(network: NetworkDto, kind: NetworkLinkKind): string[] {
  const ids = network[linkField(kind)] ?? [];
  return kind === 'network' && network.id ? [...ids, network.id] : ids;
}

/** a copy of the network with the links to the entities added */
export function addLinks(network: NetworkDto, kind: NetworkLinkKind, ids: string[]): NetworkDto {
  const field = linkField(kind);
  const current = network[field] ?? [];
  return { ...network, [field]: [...current, ...ids.filter((id) => !current.includes(id))] };
}

/** a copy of the network with the links to the entities removed */
export function removeLinks(network: NetworkDto, kind: NetworkLinkKind, ids: string[]): NetworkDto {
  const field = linkField(kind);
  return { ...network, [field]: (network[field] ?? []).filter((id) => !ids.includes(id)) };
}

/** the members of a network with a role */
export interface RoleMembers {
  role: string;
  persons: PersonDto[];
}

/**
 * The members of the network by role: the configured roles first, then the other roles found, the
 * persons ordered as in the sort order, the ones not in it last.
 */
export function membersByRole(
  persons: PersonDto[],
  networkId: string,
  roles: string[],
  sortOrder: MembershipSortOrderDto[] | undefined,
): RoleMembers[] {
  const found = persons.flatMap((person) =>
    (person.networkMemberships ?? [])
      .filter((membership) => membership.parentId === networkId)
      .map((membership) => membership.role),
  );
  const allRoles = [...new Set([...roles, ...found])];
  return allRoles.map((role) => {
    const order = sortOrder?.find((item) => item.role === role)?.personIds ?? [];
    const rank = (person: PersonDto) => {
      const index = order.indexOf(person.id ?? '');
      return index < 0 ? order.length : index;
    };
    const members = persons
      .filter((person) =>
        (person.networkMemberships ?? []).some(
          (membership) => membership.parentId === networkId && membership.role === role,
        ),
      )
      .sort((a, b) => rank(a) - rank(b));
    return { role, persons: members };
  });
}

/** the sort order of the members, with a person moved by delta positions in a role */
export function moveMember(
  members: RoleMembers[],
  role: string,
  personId: string,
  delta: number,
): MembershipSortOrderDto[] {
  return members.map((item) => {
    const personIds = item.persons.map((person) => person.id ?? '');
    if (item.role === role) {
      const from = personIds.indexOf(personId);
      const to = from + delta;
      if (from >= 0 && to >= 0 && to < personIds.length) {
        personIds.splice(to, 0, ...personIds.splice(from, 1));
      }
    }
    return { role: item.role, personIds };
  });
}

/**
 * The persons search query of the members of the entities linked to the network: its studies, the
 * studies of its networks and its networks; undefined when there are none.
 */
export function associatedPeopleQuery(network: NetworkDto): string | undefined {
  const studyIds = [
    ...new Set([
      ...(network.studyIds ?? []),
      ...(network.networkSummaries ?? []).flatMap((summary) => summary.studyIds ?? []),
    ]),
  ];
  const networkIds = network.networkIds ?? [];
  const terms = (ids: string[]) => ids.map((id) => `"${id}"`).join(' OR ');
  const parts = [
    ...(studyIds.length > 0 ? [`studyMemberships.parentId:(${terms(studyIds)})`] : []),
    ...(networkIds.length > 0 ? [`networkMemberships.parentId:(${terms(networkIds)})`] : []),
  ];
  return parts.length > 0 ? parts.join(' OR ') : undefined;
}
