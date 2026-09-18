<template>
  <div>
    <div class="text-caption text-grey-7 q-mb-sm">
      {{ t('history.revision_date') }}: {{ getDateLabel(commit.date) }} ({{ commit.author }})
      <span v-if="commit.comment" class="q-ml-sm">{{ commit.comment }}</span>
    </div>
    <q-card flat bordered>
      <q-card-section>
        <q-spinner-dots v-if="loading" color="primary" size="2em" />
        <slot v-else />
      </q-card-section>
    </q-card>
  </div>
</template>

<script setup lang="ts">
import type { GitCommitInfoDto } from 'src/models/Mica';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  commit: GitCommitInfoDto;
  loading?: boolean;
}

defineProps<Props>();
const { t } = useI18n();
</script>
