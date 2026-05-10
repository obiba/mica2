<template>
  <div>
    <div class="q-mr-md">
      <q-table
        :rows="rows"
        flat
        row-key="name"
        :columns="columns"
        :pagination="initialPagination"
        selection="multiple"
        v-model:selected="selectedTranslations"
      >
        <template v-slot:top-left>
          <div class="q-gutter-md">
            <q-btn size="sm" icon="add" color="primary" :label="t('add')" @click="onAdd" />
            <q-btn
              size="sm"
              icon="delete"
              color="negative"
              :label="t('delete')"
              :disable="selectedTranslations.length < 1"
              @click="onDelete"
            />
          </div>
        </template>
        <template v-slot:top-right>
          <q-input v-model="filter" debounce="300" :placeholder="t('search')" dense clearable class="q-mr-md">
            <template v-slot:prepend>
              <q-icon name="search" />
            </template>
          </q-input>
        </template>
        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <span>{{ props.value }}</span>
          </q-td>
        </template>
        <template v-slot:body-cell-attributes="props">
          <q-td :props="props">
            <div v-for="lang in languages" :key="lang">
              <q-input
                type="text"
                debounce="500"
                v-model="props.row.attributes[lang].value"
                dense
                @update:model-value="onValueChanged(props.row.attributes[lang])"
              >
                <template v-slot:prepend>
                  <q-chip :label="lang.toUpperCase()" size="sm" />
                </template>
              </q-input>
            </div>
          </q-td>
        </template>
      </q-table>
    </div>

    <confirm-dialog
      v-model="showDelete"
      :title="t('user.remove')"
      :text="t('system.translations.remove_confirm', { count: selectedTranslations.length })"
      @confirm="doDelete"
    />

    <system-custom-translations-dialog
      v-model="showAdd"
      :translation-keys="translationKeys"
      :languages="languages"
      @added="onAdded"
      @cancel="showAdd = false"
    />
  </div>
</template>

<script setup lang="ts">
import type { AttributeDto } from 'src/models/Mica';
import { DefaultAlignment } from 'src/components/models';
import { translationAsMap, mapAsTranslation } from 'src/utils/translations';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import SystemCustomTranslationsDialog from 'src/components/SystemCustomTranslationsDialog.vue';
import { notifyError, notifySuccess } from 'src/utils/notify';

const systemStore = useSystemStore();
const { t } = useI18n();

interface TranslationRow {
  name: string;
  attributes: Record<string, AttributeDto>;
}

const initialPagination = ref({
  descending: false,
  page: 1,
  rowsPerPage: 10,
});

const filter = ref('');
const dirty = ref(false);
const showAdd = ref(false);
const showDelete = ref(false);
const allTranslations = ref<Record<string, AttributeDto[]>>({});
const translations = ref<TranslationRow[]>([]);
const translationKeys = computed(() => (allTranslations.value[systemStore.defaultLanguage] || []).map((x) => x.name));
const selectedTranslations = ref<AttributeDto[]>([]);
const languages = computed<string[]>(() => systemStore.configuration.languages || []);
const columns = computed(() => [
  { name: 'name', label: t('name'), field: 'name', align: DefaultAlignment, sortable: true },
  { name: 'attributes', label: t('translations'), field: 'attributes', align: DefaultAlignment },
]);
const rows = computed(
  () =>
    translations.value.filter((row) =>
      filter.value ? row.name.toLowerCase().includes(filter.value.toLowerCase()) : true,
    ) || [],
);

function save() {
  dirty.value = false;
  selectedTranslations.value.splice(0);
  systemStore
    .updateTranslation(mapAsTranslation(allTranslations.value))
    .then(() => {
      systemStore.init();
      notifySuccess(t('system.translations.updated'));
    })
    .catch(() => {
      notifyError(t('system.translations.update_failed'));
    });
}

function onValueChanged(row: AttributeDto) {
  if (!row.value || row.value.length === 0) {
    row.value = row.name;
  }

  save();
}

function onAdd() {
  showAdd.value = true;
}

function onAdded(newName: string, newValues: Record<string, string>) {
  selectedTranslations.value.splice(0);
  showAdd.value = false;

  languages.value.forEach((lang) => {
    if (!allTranslations.value[lang]) {
      allTranslations.value[lang] = [];
    }

    allTranslations.value[lang].push({
      name: newName,
      value: newValues[lang] || newName,
    });
  });

  save();
}

function onDelete() {
  showDelete.value = true;
}

function doDelete() {
  selectedTranslations.value.forEach((translation) => {
    languages.value.forEach((lang) => {
      if (allTranslations.value[lang]) {
        allTranslations.value[lang] = allTranslations.value[lang].filter((x) => x.name !== translation.name);
      }
    });
  });

  selectedTranslations.value.splice(0);
  showDelete.value = false;
  save();
}

watch(
  () => systemStore.configuration.translations,
  (newValue) => {
    if (newValue) {
      allTranslations.value = translationAsMap(newValue);
      // get the list of attribute names
      const names = new Set();
      Object.keys(allTranslations.value).forEach((lang) => {
        allTranslations.value[lang]?.forEach((attr) => {
          names.add(attr.name);
        });
      });
      // for each name, map the translations to the attribute for each language
      translations.value = [];
      names.forEach((name) => {
        const row = { name, attributes: {} } as TranslationRow;
        languages.value.forEach((lang) => {
          if (allTranslations.value[lang]) {
            const attr = allTranslations.value[lang].find((x) => x.name === name);
            if (attr) {
              row.attributes[lang] = attr;
            } else {
              row.attributes[lang] = { name, value: name } as AttributeDto;
            }
          } else {
            row.attributes[lang] = { name, value: name } as AttributeDto;
          }
        });
        translations.value.push(row);
      });
    }
  },
  { immediate: true },
);
</script>
