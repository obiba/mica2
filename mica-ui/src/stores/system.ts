import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type {
  PublicMicaConfigDto,
  LocalizedStringDto,
} from 'src/models/Mica';

export const useSystemStore = defineStore('system', () => {
  const configurationPublic = ref<PublicMicaConfigDto>({} as PublicMicaConfigDto);
  const translations = ref<LocalizedStringDto[]>([]);
  const defaultLanguage = computed(() => (configurationPublic.value.languages || [])[0] || 'en');

  async function initPub() {
    return api.get('/config/_public').then((response) => {
      if (response.status === 200) {
        configurationPublic.value = response.data;
      }
      return response;
    });
  }

  async function init() {
    return api.get('/config').then((response) => {
      if (response.status === 200) {
        configurationPublic.value = response.data;
      }
      return response;
    });
  }


  return {
    translations,
    configurationPublic,
    defaultLanguage,
    init,
    initPub,
  };
});
