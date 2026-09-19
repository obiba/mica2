<template>
  <q-card flat bordered>
    <q-card-section class="row items-center bg-grey-2 q-py-xs">
      <q-icon name="comment" color="grey-7" class="q-mr-sm" />
      <div class="col text-caption text-grey-8">
        {{ t('comments.created_by') }} <strong>{{ author }}</strong>
        <span :title="getDateLabel(comment.timestamps?.created)" class="q-ml-xs">
          {{ getDateDistanceLabel(comment.timestamps?.created) }}
        </span>
        <span v-if="modified" class="q-ml-sm">
          &middot; {{ comment.modifiedBy ? t('comments.modified_by', { author: modifier }) : t('comments.modified') }}
          <span :title="getDateLabel(comment.timestamps?.lastUpdate)" class="q-ml-xs">
            {{ getDateDistanceLabel(comment.timestamps?.lastUpdate) }}
          </span>
        </span>
      </div>
      <div v-if="!editing" class="col-auto">
        <q-btn
          v-if="canAct(comment, 'EDIT')"
          flat
          dense
          round
          size="sm"
          icon="edit"
          color="primary"
          :title="t('edit')"
          :disable="disable"
          @click="editing = true"
        />
        <q-btn
          v-if="canAct(comment, 'DELETE')"
          flat
          dense
          round
          size="sm"
          icon="delete"
          color="negative"
          :title="t('delete')"
          :disable="disable"
          @click="emit('delete', comment)"
        />
      </div>
    </q-card-section>
    <q-separator />
    <q-card-section>
      <comment-editor
        v-if="editing"
        :model-value="comment.message"
        :label="t('save')"
        cancellable
        :saving="disable"
        @submit="emit('update', comment, $event)"
        @cancel="editing = false"
      />
      <markdown-text v-else :text="comment.message" />
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import type { CommentDto } from 'src/models/Mica';
import CommentEditor from 'src/components/comments/CommentEditor.vue';
import MarkdownText from 'src/components/comments/MarkdownText.vue';
import { canAct } from 'src/composables/useComments';
import { getDateDistanceLabel, getDateLabel } from 'src/utils/dates';
import { getUserDisplayName } from 'src/utils/users';

interface Props {
  comment: CommentDto;
  /** the thread is busy: a comment is being saved or deleted */
  disable?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ update: [comment: CommentDto, message: string]; delete: [comment: CommentDto] }>();
const { t } = useI18n();

const editing = ref(false);

const author = computed(() => getUserDisplayName(props.comment.createdByProfile, props.comment.createdBy));
const modifier = computed(() => getUserDisplayName(props.comment.modifiedByProfile, props.comment.modifiedBy));
/** the comment was edited after its creation */
const modified = computed(() => {
  const { created, lastUpdate } = props.comment.timestamps ?? {};
  return !!lastUpdate && lastUpdate !== created;
});

/** the thread reloads after an update: leave the edit mode when the comment comes back */
watch(
  () => props.comment,
  () => {
    editing.value = false;
  },
);
</script>
