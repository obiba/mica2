<template>
  <q-btn
    v-if="canDelete"
    color="negative"
    icon="delete"
    :label="t('delete')"
    size="sm"
    :disable="disable"
    @click="showDelete = true"
  />
  <q-btn
    v-if="canPublish"
    color="primary"
    icon="star"
    :label="t('publish.label')"
    size="sm"
    :disable="disable || !hasUnpublishedChanges"
    @click="emit('action', { type: 'publish' })"
  />
  <q-btn
    v-if="canUnpublish"
    color="primary"
    outline
    icon="star_outline"
    :label="t('publish.unpublish')"
    size="sm"
    :disable="disable"
    @click="emit('action', { type: 'unpublish' })"
  />
  <q-btn-dropdown
    v-if="canChangeStatus && status"
    :label="t(`publish.status.${status}`)"
    size="sm"
    outline
    color="grey-8"
    :disable="disable"
  >
    <q-list dense>
      <q-item v-if="canGoToDraft" clickable v-close-popup @click="emit('action', { type: 'status', status: DRAFT })">
        <q-item-section avatar><q-icon name="arrow_back" /></q-item-section>
        <q-item-section>{{ t('publish.status.DRAFT') }}</q-item-section>
      </q-item>
      <q-item
        v-if="canGoToUnderReview"
        clickable
        v-close-popup
        @click="emit('action', { type: 'status', status: UNDER_REVIEW })"
      >
        <q-item-section avatar><q-icon name="arrow_forward" /></q-item-section>
        <q-item-section>{{ t('publish.status.UNDER_REVIEW') }}</q-item-section>
      </q-item>
      <q-item v-if="canGoToDeleted" clickable v-close-popup @click="emit('action', { type: 'status', status: DELETED })">
        <q-item-section avatar><q-icon name="arrow_forward" /></q-item-section>
        <q-item-section>{{ t('publish.status.DELETED') }}</q-item-section>
      </q-item>
    </q-list>
  </q-btn-dropdown>
  <confirm-dialog
    v-model="showDelete"
    :title="t('document.delete_title')"
    :text="t('document.delete_text', { id })"
    @confirm="emit('action', { type: 'delete' })"
  />
</template>

<script setup lang="ts">
import type { EntityStateDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import { DRAFT, UNDER_REVIEW, DELETED, useDocumentState } from 'src/composables/useDocumentState';
import type { DocumentAction } from 'src/composables/useDocumentActions';

interface Props {
  /** id of the document, for the delete confirmation */
  id: string;
  state: EntityStateDto | undefined;
  /** while an action is running */
  disable?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ action: [action: DocumentAction] }>();
const { t } = useI18n();

const {
  status,
  hasUnpublishedChanges,
  canDelete,
  canPublish,
  canUnpublish,
  canChangeStatus,
  canGoToDraft,
  canGoToUnderReview,
  canGoToDeleted,
} = useDocumentState(() => props.state);

const showDelete = ref(false);
</script>
