<template>
  <span v-if="state" class="inline-block">
    <q-icon :name="icon" :color="color" size="sm">
      <q-tooltip>{{ tooltip }}</q-tooltip>
    </q-icon>
    <q-badge v-if="isPublishedOutOfDate" color="info" class="q-ml-xs">
      {{ revisionsAhead }}
      <q-tooltip>{{ t('publish.revisions_ahead') }}</q-tooltip>
    </q-badge>
    <q-chip v-if="showStatus && status" dense square :color="statusColor" text-color="white" class="q-ml-sm">
      {{ t(`publish.status.${status}`) }}
    </q-chip>
  </span>
</template>

<script setup lang="ts">
import type { EntityStateDto } from 'src/models/Mica';
import { useDocumentState } from 'src/composables/useDocumentState';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  state: EntityStateDto | undefined;
  /** also show the revision status (draft, under review, deleted) as a chip */
  showStatus?: boolean;
}

const props = defineProps<Props>();
const { t } = useI18n();

const { status, isPublished, isPublishedOutOfDate, revisionsAhead } = useDocumentState(() => props.state);

const icon = computed(() => (!isPublished.value ? 'star_outline' : isPublishedOutOfDate.value ? 'star_half' : 'star'));
const color = computed(() => (isPublished.value ? 'warning' : 'grey-6'));

const publication = computed(() =>
  props.state ? `${getDateLabel(props.state.publicationDate)} [${props.state.publishedBy ?? ''}]` : '',
);
const tooltip = computed(() =>
  !isPublished.value
    ? t('publish.not_published')
    : isPublishedOutOfDate.value
      ? `${t('publish.published_out_of_date')} (${publication.value})`
      : `${t('publish.published')} (${publication.value})`,
);

const statusColor = computed(() =>
  status.value === 'DRAFT' ? 'grey-7' : status.value === 'UNDER_REVIEW' ? 'primary' : 'negative',
);
</script>
