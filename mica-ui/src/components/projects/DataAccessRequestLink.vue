<template>
  <span v-if="request" class="inline-block">
    <a v-if="request.viewable" :href="toPortalUrl(`/data-access/${request.id}`)" class="text-primary">{{
      request.id
    }}</a>
    <span v-else>{{ request.id }}</span>
    <q-chip dense square :color="statusColor" text-color="white" class="q-ml-sm">
      {{ t(`data_access_request.status.${request.status}`) }}
    </q-chip>
  </span>
</template>

<script setup lang="ts">
import type { DataAccessRequestSummaryDto } from 'src/models/Mica';
import { toPortalUrl } from 'src/boot/api';

interface Props {
  /** the data access request a project comes from, if any */
  request: DataAccessRequestSummaryDto | undefined;
}

const props = defineProps<Props>();
const { t } = useI18n();

const STATUS_COLORS: Record<string, string> = {
  OPENED: 'grey-7',
  SUBMITTED: 'info',
  REVIEWED: 'primary',
  CONDITIONALLY_APPROVED: 'warning',
  APPROVED: 'positive',
  REJECTED: 'negative',
};

const statusColor = computed(() => STATUS_COLORS[props.request?.status ?? ''] ?? 'grey-7');
</script>
