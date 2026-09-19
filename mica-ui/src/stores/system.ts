import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type { MicaConfigDto, PublicMicaConfigDto, LocalizedStringDto } from 'src/models/Mica';
import type { TaxonomiesDto, TaxonomySummaryDto } from 'src/models/Opal';

export const useSystemStore = defineStore('system', () => {
  const configurationPublic = ref<PublicMicaConfigDto>({} as PublicMicaConfigDto);
  /** the full configuration, for authenticated users (roles, open access, languages, document options) */
  const configuration = ref<MicaConfigDto>({} as MicaConfigDto);
  const translations = ref<LocalizedStringDto[]>([]);
  const defaultLanguage = computed(() => (configurationPublic.value.languages || [])[0] || 'en');
  const languages = computed(() => configuration.value.languages || configurationPublic.value.languages || ['en']);
  /** the ISO languages (code: display name), by the locale of the display names */
  const availableLanguages = ref<Record<string, Record<string, string>>>({});
  /** the summaries (name, titles) of the Opal taxonomies, for the concept tagging option */
  const taxonomiesSummary = ref<TaxonomySummaryDto[]>([]);

  async function initPub() {
    return api.get('/config/_public').then((response) => {
      if (response.status === 200) {
        configurationPublic.value = response.data;
      }
      return response;
    });
  }

  async function init() {
    return api.get<MicaConfigDto>('/config').then((response) => {
      if (response.status === 200) {
        configuration.value = response.data;
      }
      return response;
    });
  }

  /** replaces the whole configuration, then reloads it (and its public view) so that the app follows */
  async function save(config: MicaConfigDto): Promise<void> {
    await api.put('/config', config);
    await Promise.all([init(), initPub()]);
  }

  async function loadLanguages(locale: string): Promise<Record<string, string>> {
    const cached = availableLanguages.value[locale];
    if (cached) return cached;
    const response = await api.get<Record<string, string>>('/config/languages', { params: { locale } });
    availableLanguages.value[locale] = response.data;
    return response.data;
  }

  async function loadTaxonomiesSummary(): Promise<TaxonomySummaryDto[]> {
    const response = await api.get<TaxonomiesDto>('/taxonomies/_summary', { params: { vocabularies: false } });
    taxonomiesSummary.value = response.data.summaries || [];
    return taxonomiesSummary.value;
  }

  return {
    translations,
    configurationPublic,
    configuration,
    defaultLanguage,
    languages,
    availableLanguages,
    taxonomiesSummary,
    init,
    initPub,
    save,
    loadLanguages,
    loadTaxonomiesSummary,
  };
});
