<template>
  <q-card flat bordered>
    <q-card-section>
      <q-input
        v-model="text"
        dense
        outlined
        clearable
        :placeholder="t('files.search')"
        @keyup.enter="onSearch"
        @keyup.esc="onClear"
        @clear="onClear"
      >
        <template v-slot:prepend>
          <q-icon name="search" />
        </template>
      </q-input>
      <q-checkbox
        :model-value="recursively"
        dense
        size="sm"
        :label="t('files.recursively_hint')"
        class="q-mt-sm text-caption"
        @update:model-value="emit('update:recursively', $event)"
      />
    </q-card-section>
    <q-separator />
    <q-list dense>
      <q-item-label header class="q-pb-xs">{{ t('files.shortcuts') }}</q-item-label>
      <q-item
        v-for="shortcut in FILE_SEARCH_SHORTCUTS"
        :key="shortcut"
        clickable
        :active="search?.shortcut === shortcut"
        active-class="text-primary text-weight-medium"
        @click="emit('shortcut', shortcut)"
      >
        <q-item-section side><q-icon :name="SHORTCUT_ICONS[shortcut]" size="xs" /></q-item-section>
        <q-item-section>{{ t(`files.shortcut.${shortcut}`) }}</q-item-section>
      </q-item>
      <q-item v-if="search" clickable @click="onClear">
        <q-item-section side><q-icon name="filter_alt_off" size="xs" /></q-item-section>
        <q-item-section>{{ t('files.clear_search') }}</q-item-section>
      </q-item>
    </q-list>
  </q-card>
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
