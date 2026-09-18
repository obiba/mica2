<template>
  <q-card flat bordered>
    <q-card-section class="row items-center no-wrap">
      <q-icon :name="fileIcon(document)" size="sm" class="q-mr-sm" />
      <div class="ellipsis text-subtitle2" :title="document.name">{{ document.name || '/' }}</div>
      <q-space />
      <q-btn
        v-if="file && canEdit(document)"
        flat
        dense
        round
        size="sm"
        icon="edit"
        color="primary"
        :title="t('files.edit_details')"
        @click="emit('edit')"
      />
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
        <q-item v-if="file">
          <q-item-section>
            <q-item-label caption>{{ t('files.description') }}</q-item-label>
            <q-item-label v-if="descriptions.length === 0" class="text-grey-6">{{
              t('files.no_description')
            }}</q-item-label>
            <q-item-label v-for="entry in descriptions" :key="entry.lang" class="q-mt-xs">
              <q-badge outline color="grey-7" class="q-mr-xs">{{ entry.lang.toUpperCase() }}</q-badge>
              <span style="white-space: pre-line">{{ entry.value }}</span>
            </q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-card-section>
    <slot />
  </q-card>
</template>

<script setup lang="ts">
import type { FileDto } from 'src/models/Mica';
import { canEdit, fileIcon, isFile, isFolder, isPublished, sizeLabel } from 'src/utils/files';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  document: FileDto;
}

const props = defineProps<Props>();
const emit = defineEmits<{ edit: [] }>();
const { t } = useI18n();

const file = computed(() => isFile(props.document));
const descriptions = computed(() =>
  (props.document.state?.attachment?.description ?? []).filter((entry) => entry.value?.trim()),
);

const items = computed(() => [
  {
    label: t('files.size'),
    value: isFolder(props.document)
      ? t('files.items', { count: props.document.size ?? 0 })
      : sizeLabel(props.document.size),
  },
  { label: t('created'), value: getDateLabel(props.document.timestamps?.created) },
  { label: t('last_modified'), value: getDateLabel(props.document.timestamps?.lastUpdate) },
  { label: t('status'), value: t(`publish.status.${props.document.revisionStatus}`) },
  {
    label: t('publish.publication'),
    value: isPublished(props.document)
      ? `${getDateLabel(props.document.state?.publicationDate)} [${props.document.state?.publishedBy ?? ''}]`
      : t('publish.not_published'),
  },
  ...(file.value ? [{ label: t('files.type'), value: props.document.state?.attachment?.type || '-' }] : []),
]);
</script>
