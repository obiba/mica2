<template>
  <div class="row items-center q-gutter-xs">
    <q-btn v-if="some(canDelete)" color="negative" icon="delete" :label="t('delete')" size="sm" :disable="disable" @click="showDelete = true" />
    <q-btn v-if="some(canPublish)" color="primary" icon="star" :label="t('publish.label')" size="sm" :disable="disable" @click="emit('publish', true)" />
    <q-btn v-if="some(canUnpublish)" color="primary" outline icon="star_outline" :label="t('publish.unpublish')" size="sm" :disable="disable" @click="emit('publish', false)" />
    <q-btn-dropdown
      v-if="document.permissions?.edit && (some(toDraft) || some(toUnderReview) || some(toDeleted))"
      :label="selection.length > 0 ? t('files.selection_status') : t(`publish.status.${document.revisionStatus}`)"
      size="sm"
      outline
      color="grey-8"
      :disable="disable"
    >
      <q-list dense>
        <q-item v-if="some(toDraft)" clickable v-close-popup @click="emit('status', FILE_DRAFT)">
          <q-item-section avatar><q-icon name="arrow_back" /></q-item-section>
          <q-item-section>{{ t('publish.status.DRAFT') }}</q-item-section>
        </q-item>
        <q-item v-if="some(toUnderReview)" clickable v-close-popup @click="emit('status', FILE_UNDER_REVIEW)">
          <q-item-section avatar><q-icon name="arrow_forward" /></q-item-section>
          <q-item-section>{{ t('publish.status.UNDER_REVIEW') }}</q-item-section>
        </q-item>
        <q-item v-if="some(toDeleted)" clickable v-close-popup @click="emit('status', FILE_DELETED)">
          <q-item-section avatar><q-icon name="arrow_forward" /></q-item-section>
          <q-item-section>{{ t('publish.status.DELETED') }}</q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>
    <confirm-dialog
      v-model="showDelete"
      :title="t('files.delete_title')"
      :text="t('files.delete_text', { count: targets.filter(canDelete).length })"
      @confirm="emit('delete')"
    />
  </div>
</template>

<script setup lang="ts">
import type { FileDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import type { FileStatus } from 'src/utils/files';
import { FILE_DELETED, FILE_DRAFT, FILE_UNDER_REVIEW, canDelete, canGoTo, canPublish, canUnpublish } from 'src/utils/files';

interface Props {
  /** the current document */
  document: FileDto;
  /** the selected children, when any: the operations apply to them instead */
  selection: FileDto[];
  disable?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ publish: [value: boolean]; status: [status: FileStatus]; delete: [] }>();
const { t } = useI18n();

const showDelete = ref(false);
const targets = computed(() => (props.selection.length > 0 ? props.selection : [props.document]));

function some(predicate: (file: FileDto) => boolean) {
  return targets.value.some(predicate);
}
const toDraft = (file: FileDto) => canGoTo(file, FILE_DRAFT);
const toUnderReview = (file: FileDto) => canGoTo(file, FILE_UNDER_REVIEW);
const toDeleted = (file: FileDto) => canGoTo(file, FILE_DELETED);
</script>
