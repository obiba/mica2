import type { NetworkDto } from 'src/models/Mica';
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
