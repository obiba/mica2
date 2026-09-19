import type { MicaConfigDto } from 'src/models/Mica';
import type { LocaleTextDto } from 'src/models/Opal';

export const OPAL_VIEWS_GROUPINGS = ['PROJECT_TABLE', 'PROJECT_ENTITY_TYPE', 'ENTITY_TYPE'] as const;

export const SUMMARY_STATISTICS_ACCESS_POLICIES = [
  'OPEN_ALL',
  'OPEN_SUMMARY',
  'OPEN_BASICS',
  'RESTRICTED_ALL',
] as const;

/** a working copy of the configuration, for a section dialog */
export function copyConfig(config: MicaConfigDto): MicaConfigDto {
  return JSON.parse(JSON.stringify(config));
}

/**
 * The flags that depend on another one: the counts of projects and of data access requests are
 * not shown when the section is disabled.
 */
export function applyFeatureDependencies(config: MicaConfigDto): MicaConfigDto {
  if (!config.isProjectEnabled) config.isProjectsCountEnabled = false;
  if (!config.isDataAccessEnabled) config.isDataAccessRequestsCountEnabled = false;
  return config;
}

/** any cart: the cart life span applies */
export function isAnyCartEnabled(config: MicaConfigDto): boolean {
  return config.isCartEnabled === true || config.isStudiesCartEnabled === true || config.isNetworksCartEnabled === true;
}

/** the group names typed in one go, space separated as in the legacy form */
export function splitGroups(text: string): string[] {
  return text
    .split(/\s+/)
    .map((group) => group.trim())
    .filter((group) => group.length > 0);
}

/** the languages of `next` that are not in `current` */
export function addedLanguages(current: string[], next: string[]): string[] {
  return next.filter((lang) => !current.includes(lang));
}

/**
 * The text in the locale, else in english, else the first one, else the fallback (the taxonomy
 * name for instance).
 */
export function localeText(texts: LocaleTextDto[] | undefined, locale: string, fallback = ''): string {
  if (!texts || texts.length === 0) return fallback;
  const found = texts.find((text) => text.locale === locale) || texts.find((text) => text.locale === 'en') || texts[0];
  return found?.text || fallback;
}

/** the membership roles are lowercase identifiers: no uppercase, whitespace, `~` or `!` */
export function hasRoleSpecialCharacters(role: string): boolean {
  return /[A-Z\s~!]/.test(role);
}

export function isValidRoleId(role: string, roles: string[], oldRole?: string): boolean {
  const id = role.trim();
  if (id.length === 0 || hasRoleSpecialCharacters(id)) return false;
  return id === oldRole || !roles.includes(id);
}
