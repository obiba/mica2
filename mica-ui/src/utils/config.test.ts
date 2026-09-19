import { describe, expect, it } from 'vitest';
import type { MicaConfigDto } from 'src/models/Mica';
import {
  addedLanguages,
  applyFeatureDependencies,
  copyConfig,
  hasRoleSpecialCharacters,
  isAnyCartEnabled,
  isValidRoleId,
  localeText,
  splitGroups,
} from './config';

function config(partial: Partial<MicaConfigDto>): MicaConfigDto {
  return { name: 'Mica', languages: ['en'], roles: [], ...partial } as MicaConfigDto;
}

describe('config utils', () => {
  it('copies the configuration deeply', () => {
    const original = config({ languages: ['en', 'fr'] });
    const copy = copyConfig(original);
    copy.languages.push('de');
    expect(original.languages).toEqual(['en', 'fr']);
  });

  it('clears the counts of the disabled sections', () => {
    const result = applyFeatureDependencies(
      config({
        isProjectEnabled: false,
        isProjectsCountEnabled: true,
        isDataAccessEnabled: true,
        isDataAccessRequestsCountEnabled: true,
      }),
    );
    expect(result.isProjectsCountEnabled).toBe(false);
    expect(result.isDataAccessRequestsCountEnabled).toBe(true);
  });

  it('tells whether any cart is enabled', () => {
    expect(
      isAnyCartEnabled(config({ isCartEnabled: false, isStudiesCartEnabled: false, isNetworksCartEnabled: false })),
    ).toBe(false);
    expect(isAnyCartEnabled(config({ isCartEnabled: false, isStudiesCartEnabled: true }))).toBe(true);
  });

  it('splits the groups on whitespace', () => {
    expect(splitGroups('  mica-user   group-a\tgroup-b ')).toEqual(['mica-user', 'group-a', 'group-b']);
    expect(splitGroups('')).toEqual([]);
  });

  it('finds the added languages', () => {
    expect(addedLanguages(['en'], ['en', 'fr'])).toEqual(['fr']);
    expect(addedLanguages(['en', 'fr'], ['en'])).toEqual([]);
  });

  it('picks the text in the locale with fallbacks', () => {
    const texts = [
      { locale: 'en', text: 'Areas' },
      { locale: 'fr', text: 'Domaines' },
    ];
    expect(localeText(texts, 'fr')).toBe('Domaines');
    expect(localeText(texts, 'de')).toBe('Areas');
    expect(localeText([{ locale: 'es', text: 'Areas' }], 'de')).toBe('Areas');
    expect(localeText(undefined, 'en', 'name')).toBe('name');
    expect(localeText([], 'en', 'name')).toBe('name');
  });

  it('validates the role identifiers', () => {
    expect(hasRoleSpecialCharacters('Investigator')).toBe(true);
    expect(hasRoleSpecialCharacters('data manager')).toBe(true);
    expect(hasRoleSpecialCharacters('data-manager')).toBe(false);
    expect(isValidRoleId('data-manager', ['investigator'])).toBe(true);
    expect(isValidRoleId('investigator', ['investigator'])).toBe(false);
    expect(isValidRoleId('investigator', ['investigator'], 'investigator')).toBe(true);
    expect(isValidRoleId('  ', [])).toBe(false);
    expect(isValidRoleId('Bad!', [])).toBe(false);
  });
});
