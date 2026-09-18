<template>
  <div class="row items-center q-gutter-sm">
    <q-input
      v-model="text"
      dense
      outlined
      clearable
      :placeholder="t('files.search')"
      class="col-12 col-sm-4"
      @keyup.enter="onSearch"
      @keyup.esc="onClear"
      @clear="onClear"
    >
      <template v-slot:prepend>
        <q-icon name="search" />
      </template>
    </q-input>
    <q-btn
      flat
      dense
      size="sm"
      icon="account_tree"
      :label="t('files.recursively')"
      :color="recursively ? 'primary' : 'grey-7'"
      :title="t('files.recursively_hint')"
      @click="emit('update:recursively', !recursively)"
    />
    <q-btn-dropdown flat dense size="sm" icon="bolt" :label="t('files.shortcuts')" color="grey-8">
      <q-list dense>
        <q-item
          v-for="shortcut in FILE_SEARCH_SHORTCUTS"
          :key="shortcut"
          clickable
          v-close-popup
          @click="emit('shortcut', shortcut)"
        >
          <q-item-section avatar><q-icon :name="SHORTCUT_ICONS[shortcut]" /></q-item-section>
          <q-item-section>{{ t(`files.shortcut.${shortcut}`) }}</q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>
    <q-chip v-if="search" removable dense color="primary" text-color="white" icon="filter_alt" @remove="onClear">
      {{ search.shortcut ? t(`files.shortcut.${search.shortcut}`) : search.query }}
    </q-chip>
  </div>
</template>

<script setup lang="ts">
import { FILE_SEARCH_SHORTCUTS, type FileSearch, type FileSearchShortcut } from 'src/composables/useFileSystem';

interface Props {
  /** the search in progress, if any */
  search?: FileSearch | undefined;
  recursively: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  search: [query: string];
  shortcut: [shortcut: FileSearchShortcut];
  clear: [];
  'update:recursively': [value: boolean];
}>();
const { t } = useI18n();

const SHORTCUT_ICONS: Record<FileSearchShortcut, string> = {
  NOT_PUBLISHED: 'star_outline',
  UNDER_REVIEW: 'visibility',
  DELETED: 'delete',
  RECENT: 'schedule',
};

const text = ref('');

watch(
  () => props.search,
  (value) => {
    if (!value) text.value = '';
    else if (!value.shortcut) text.value = value.query;
  },
);

function onSearch() {
  if (text.value?.trim()) emit('search', text.value.trim());
  else onClear();
}

function onClear() {
  text.value = '';
  emit('clear');
}
</script>
