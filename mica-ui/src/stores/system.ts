import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type { MicaConfigDto, PublicMicaConfigDto, LocalizedStringDto } from 'src/models/Mica';

export const useSystemStore = defineStore('system', () => {
  const configurationPublic = ref<PublicMicaConfigDto>({} as PublicMicaConfigDto);
  /** the full configuration, for authenticated users (roles, open access, languages, document options) */
  const configuration = ref<MicaConfigDto>({} as MicaConfigDto);
  const translations = ref<LocalizedStringDto[]>([]);
  const defaultLanguage = computed(() => (configurationPublic.value.languages || [])[0] || 'en');
  const languages = computed(() => configuration.value.languages || configurationPublic.value.languages || ['en']);

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

  return {
    translations,
    configurationPublic,
    configuration,
    defaultLanguage,
    languages,
    init,
    initPub,
  };
});
