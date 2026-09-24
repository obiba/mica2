import { localizedToArray, localizedToObject, type FormModel } from 'src/composables/useDocumentModel';
import type { DocumentType } from 'src/composables/useDocumentTarget';
import {
  PersonDto_Type,
  type LocalizedStringDto,
  type MembershipSortOrderDto,
  type PersonDto,
  type PersonDto_MembershipDto,
  type PersonsDto,
} from 'src/models/Mica';

/** the kinds of entities a person can be member of, as listed in the person pages */
export type MembershipKind = 'network' | 'study' | 'initiative';

export const MEMBERSHIP_KINDS: MembershipKind[] = ['study', 'initiative', 'network'];

/** the membership type of each kind, the person DTO field holding it, the type and the title of the entities */
export const MEMBERSHIP_INFO: Record<
  MembershipKind,
  {
    type: PersonDto_Type;
    field: 'studyMemberships' | 'networkMemberships';
    documentType: DocumentType;
    /** i18n key of the entities title */
    title: string;
  }
> = {
  study: {
    type: PersonDto_Type.STUDY,
    field: 'studyMemberships',
    documentType: 'individual-study',
    title: 'individual.studies.title',
  },
  initiative: {
    type: PersonDto_Type.INITIATIVE,
    field: 'studyMemberships',
    documentType: 'harmonization-study',
    title: 'harmonization.studies.title',
  },
  network: {
    type: PersonDto_Type.NETWORK,
    field: 'networkMemberships',
    documentType: 'network',
    title: 'networks.title',
  },
};

const IDENTIFICATION_FIELDS = ['title', 'firstName', 'lastName', 'academicLevel', 'email', 'phone'] as const;

/** the value of a localized string in a language, else the first one */
export function localized(values: LocalizedStringDto[] | undefined, lang: string): string {
  return values?.find((entry) => entry.lang === lang)?.value ?? values?.[0]?.value ?? '';
}

export function fullName(person: PersonDto | undefined): string {
  return `${person?.firstName ?? ''} ${person?.lastName ?? ''}`.trim();
}

/** the form model of a person: the localized strings as `{lang: value}`, the country as its ISO code */
export function toPersonModel(person: PersonDto): FormModel {
  const model: FormModel = {};
  IDENTIFICATION_FIELDS.forEach((field) => {
    if (person[field]) model[field] = person[field];
  });
  const institution = person.institution;
  if (institution) {
    const address = institution.address;
    model.institution = {
      name: localizedToObject(institution.name),
      department: localizedToObject(institution.department),
      ...(address
        ? {
            address: {
              street: localizedToObject(address.street),
              city: localizedToObject(address.city),
              ...(address.zip ? { zip: address.zip } : {}),
              ...(address.state ? { state: address.state } : {}),
              ...(address.country?.iso ? { country: address.country.iso } : {}),
            },
          }
        : {}),
    };
  }
  return model;
}

/** a copy of the person (id, memberships...) updated from the form model */
export function fromPersonModel(person: PersonDto, model: FormModel): PersonDto {
  const updated: PersonDto = { ...person };
  IDENTIFICATION_FIELDS.forEach((field) => {
    const value = model[field];
    if (typeof value === 'string' && value.trim() !== '') updated[field] = value.trim();
    else if (field !== 'lastName') delete updated[field];
  });
  if (typeof model.lastName !== 'string') updated.lastName = '';
  const institution = (model.institution ?? {}) as Record<string, unknown>;
  const address = (institution.address ?? {}) as Record<string, unknown>;
  const text = (value: unknown) => (typeof value === 'string' && value.trim() !== '' ? value.trim() : undefined);
  const dtoAddress = {
    street: localizedToArray(address.street) ?? [],
    city: localizedToArray(address.city) ?? [],
    zip: text(address.zip),
    state: text(address.state),
    country: text(address.country) ? { iso: text(address.country) as string, name: [] } : undefined,
  };
  const dtoInstitution = {
    name: localizedToArray(institution.name) ?? [],
    department: localizedToArray(institution.department) ?? [],
    address: dtoAddress,
  };
  const hasAddress =
    dtoAddress.street.length > 0 ||
    dtoAddress.city.length > 0 ||
    !!dtoAddress.zip ||
    !!dtoAddress.state ||
    !!dtoAddress.country;
  if (!hasAddress) delete (dtoInstitution as { address?: unknown }).address;
  if (dtoInstitution.name.length > 0 || dtoInstitution.department.length > 0 || hasAddress) {
    updated.institution = JSON.parse(JSON.stringify(dtoInstitution));
  } else {
    delete updated.institution;
  }
  return updated;
}

/** an entity a person is member of, with the person's roles in it */
export interface MembershipEntity {
  id: string;
  acronym: string;
  name: string;
  roles: string[];
  route: string;
}

/** the memberships of a kind, one entry per entity, ordered by acronym */
export function groupMemberships(person: PersonDto, kind: MembershipKind, lang: string): MembershipEntity[] {
  const info = MEMBERSHIP_INFO[kind];
  const entities = new Map<string, MembershipEntity>();
  (person[info.field] ?? [])
    .filter((membership) => kind === 'network' || membership.type === info.type)
    .forEach((membership) => {
      const entity = entities.get(membership.parentId);
      if (entity) {
        entity.roles = [...entity.roles, membership.role].sort();
      } else {
        entities.set(membership.parentId, {
          id: membership.parentId,
          acronym: localized(membership.parentAcronym, lang),
          name: localized(membership.parentName, lang),
          roles: [membership.role],
          route: `/${info.documentType}/${membership.parentId}`,
        });
      }
    });
  return [...entities.values()].sort((a, b) => a.acronym.localeCompare(b.acronym));
}

/** the entity (a network or a study summary) a membership refers to */
export interface MembershipParent {
  id: string;
  acronym: LocalizedStringDto[];
  name: LocalizedStringDto[];
}

/**
 * A copy of the person where the roles in the entities are the given ones: the memberships of these
 * entities are replaced (none when the roles are empty, i.e. removed).
 */
export function withMemberships(
  person: PersonDto,
  kind: MembershipKind,
  parents: MembershipParent[],
  roles: string[],
): PersonDto {
  const info = MEMBERSHIP_INFO[kind];
  const ids = new Set(parents.map((parent) => parent.id));
  const kept = (person[info.field] ?? []).filter((membership) => !ids.has(membership.parentId));
  const added: PersonDto_MembershipDto[] = parents.flatMap((parent) =>
    roles.map((role) => ({
      role,
      parentId: parent.id,
      parentAcronym: parent.acronym,
      parentName: parent.name,
      ...(kind === 'network' ? {} : { type: info.type }),
    })),
  );
  return { ...person, [info.field]: [...kept, ...added] };
}

/** the search query of the persons with the same first and last names, empty when there is no name */
export function duplicateQuery(person: PersonDto): string {
  return (['firstName', 'lastName'] as const)
    .filter((field) => person[field]?.trim())
    .map((field) => `${field}:${(person[field] as string).trim().replaceAll(' ', '\\ ')}`)
    .join(' AND ');
}

export interface DuplicateCheck {
  /** another person has the same email: not to be saved */
  sameEmail?: string | undefined;
  /** other persons have the same name: to be confirmed */
  sameName?: string | undefined;
}

/** what the search of the persons with the same name tells about the person to be saved */
export function checkDuplicates(person: PersonDto, found: PersonsDto | undefined): DuplicateCheck {
  const others = (found?.persons ?? []).filter((other) => other.id !== person.id);
  const email = person.email?.trim().toLowerCase();
  return {
    sameEmail: email && others.some((other) => other.email?.trim().toLowerCase() === email) ? person.email : undefined,
    sameName: others.length > 0 ? fullName(person) : undefined,
  };
}

const LUCENE_SPECIAL = /[+\-&|!(){}[\]^"~*?:\\/]/g;

/**
 * The search query of a free text: each word is a prefix (special characters escaped), a quoted
 * text is searched as it is.
 */
export function searchQuery(text: string | undefined): string | undefined {
  const trimmed = text?.trim() ?? '';
  if (trimmed === '') return undefined;
  if (/^".+"$/.test(trimmed)) return trimmed;
  return trimmed
    .split(/\s+/)
    .map((word) => `${word.replace(LUCENE_SPECIAL, '\\$&')}*`)
    .join(' ');
}

/** the entities with members: the network ones, the study ones (individual studies and initiatives) */
export type MembersParent = 'network' | 'study';

/** the members of a network or study with a role */
export interface RoleMembers {
  role: string;
  persons: PersonDto[];
}

/**
 * The members of the network or study by role: the configured roles first, then the other roles found, the
 * persons ordered as in the sort order, the ones not in it last.
 */
export function membersByRole(
  persons: PersonDto[],
  parent: MembersParent,
  parentId: string,
  roles: string[],
  sortOrder: MembershipSortOrderDto[] | undefined,
): RoleMembers[] {
  const found = persons.flatMap((person) =>
    (person[`${parent}Memberships`] ?? [])
      .filter((membership) => membership.parentId === parentId)
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
        (person[`${parent}Memberships`] ?? []).some(
          (membership) => membership.parentId === parentId && membership.role === role,
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
