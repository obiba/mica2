<template>
  <q-card flat bordered>
    <q-card-section class="row items-center no-wrap">
      <q-icon :name="fileIcon(document)" size="sm" class="q-mr-sm" />
      <div class="ellipsis text-subtitle2" :title="document.name">{{ document.name || '/' }}</div>
    </q-card-section>
    <q-separator />
    <q-card-section>
      <q-list dense>
        <q-item v-for="item in items" :key="item.label">
          <q-item-section>
            <q-item-label caption>{{ item.label }}</q-item-label>
            <q-item-label>{{ item.value }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-card-section>
    <slot />
  </q-card>
</template>

<script setup lang="ts">
import type { FileDto } from 'src/models/Mica';
import { fileIcon, isFolder, isPublished, sizeLabel } from 'src/utils/files';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  document: FileDto;
}

const props = defineProps<Props>();
const { t } = useI18n();

const items = computed(() => [
  { label: t('files.size'), value: isFolder(props.document) ? t('files.items', { count: props.document.size ?? 0 }) : sizeLabel(props.document.size) },
  { label: t('created'), value: getDateLabel(props.document.timestamps?.created) },
  { label: t('last_modified'), value: getDateLabel(props.document.timestamps?.lastUpdate) },
  { label: t('status'), value: t(`publish.status.${props.document.revisionStatus}`) },
  {
    label: t('publish.publication'),
    value: isPublished(props.document)
      ? `${getDateLabel(props.document.state?.publicationDate)} [${props.document.state?.publishedBy ?? ''}]`
      : t('publish.not_published'),
  },
]);
</script>
