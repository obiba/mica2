<template>
  <config-section :title="t('config.content_edition')" :items="items">
    <template #form="{ form }">
      <q-select
        v-model="form.languages"
        :options="languageOptions"
        multiple
        use-chips
        emit-value
        map-options
        dense
        outlined
        use-input
        input-debounce="0"
        :label="t('config.languages') + ' *'"
        :hint="t('config.languages_help')"
        :rules="[(value: string[]) => value.length > 0 || t('required')]"
        lazy-rules
        class="q-mb-md"
        @filter="onFilterLanguages"
        @update:model-value="onLanguagesChanged"
      />
      <config-input v-model="form.defaultCharSet" name="default_char_set" required no-help />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import { formattedItem, valueItem } from 'src/components/settings/general/fields';
import type { MicaConfigDto } from 'src/models/Mica';
import { addedLanguages } from 'src/utils/config';
import { notifyError, notifyWarning } from 'src/utils/notify';

const { t, locale } = useI18n();
const systemStore = useSystemStore();

/** the ISO languages named in the locale of the UI */
const languageNames = computed(() => systemStore.availableLanguages[locale.value] || {});

function languageLabel(code: string) {
  const name = languageNames.value[code];
  return name ? `${name} (${code})` : code;
}

const allLanguageOptions = computed(() =>
  Object.keys(languageNames.value)
    .map((code) => ({ label: languageLabel(code), value: code }))
    .sort((a, b) => a.label.localeCompare(b.label)),
);
const languageFilter = ref('');
const languageOptions = computed(() => {
  const needle = languageFilter.value.toLowerCase();
  return needle
    ? allLanguageOptions.value.filter((option) => option.label.toLowerCase().includes(needle))
    : allLanguageOptions.value;
});

function onFilterLanguages(value: string, update: (callback: () => void) => void) {
  update(() => {
    languageFilter.value = value;
  });
}

/** a language added to the content requires a re-indexing */
function onLanguagesChanged(value: string[]) {
  if (addedLanguages(systemStore.configuration.languages || [], value).length > 0) {
    notifyWarning('config.languages_update_warning');
  }
}

const items = computed(() => [
  formattedItem('languages', 'languages', (config: MicaConfigDto) =>
    (config.languages || []).map(languageLabel).join(', '),
  ),
  valueItem('defaultCharSet', 'default_char_set', false),
]);

watch(locale, (value) => systemStore.loadLanguages(value).catch(notifyError), { immediate: true });
</script>
