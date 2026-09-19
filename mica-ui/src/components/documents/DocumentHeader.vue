<template>
  <div class="row items-center q-mb-md">
    <div class="col">
      <div class="text-caption text-grey-7">
        {{ t('last_modified') }}: {{ getDateLabel(timestamps?.lastUpdate) }}
        <document-status-badge :state="state" class="q-ml-sm" />
      </div>
      <slot name="info" />
    </div>
    <div class="col-auto row items-center q-gutter-sm">
      <slot name="actions" />
      <document-status-buttons :id="id" :state="state" :disable="disable" @action="emit('action', $event)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { EntityStateDto, TimestampsDto } from 'src/models/Mica';
import DocumentStatusBadge from 'src/components/documents/DocumentStatusBadge.vue';
import DocumentStatusButtons from 'src/components/documents/DocumentStatusButtons.vue';
import type { DocumentAction } from 'src/composables/useDocumentActions';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  id: string;
  timestamps: TimestampsDto | undefined;
  state: EntityStateDto | undefined;
  disable?: boolean;
}

defineProps<Props>();
const emit = defineEmits<{ action: [action: DocumentAction] }>();
const { t } = useI18n();
</script>
