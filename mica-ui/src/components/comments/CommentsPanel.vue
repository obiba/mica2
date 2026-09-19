<template>
  <div>
    <q-spinner-dots v-if="loading && comments.length === 0" color="primary" size="2em" />
    <div v-else-if="comments.length === 0" class="text-grey-7 q-mb-md">{{ t('comments.none') }}</div>
    <div v-else class="q-gutter-y-md q-mb-lg">
      <comment-card
        v-for="(comment, index) in comments"
        :key="comment.id ?? index"
        :comment="comment"
        :disable="busy"
        @update="onUpdate"
        @delete="onDeleteRequest"
      />
    </div>
    <comment-editor ref="editor" :saving="busy" @submit="onAdd" />
    <confirm-dialog v-model="showDelete" :title="t('comments.delete_title')" :text="deleteText" @confirm="onDelete" />
  </div>
</template>

<script setup lang="ts">
import type { CommentDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import CommentCard from 'src/components/comments/CommentCard.vue';
import CommentEditor from 'src/components/comments/CommentEditor.vue';
import { useComments } from 'src/composables/useComments';
import { getUserDisplayName } from 'src/utils/users';

interface Props {
  /** REST path of the commented resource: `/draft/network/{id}` */
  path: string;
}

const props = defineProps<Props>();
const { t } = useI18n();

const { comments, loading, load, add, update, remove } = useComments(() => props.path);

const editor = ref<InstanceType<typeof CommentEditor>>();
const busy = ref(false);
const showDelete = ref(false);
const toDelete = ref<CommentDto>();

const deleteText = computed(() =>
  toDelete.value
    ? t('comments.delete_text', {
        author: getUserDisplayName(toDelete.value.createdByProfile, toDelete.value.createdBy),
      })
    : '',
);

async function onAdd(message: string) {
  busy.value = true;
  try {
    if (await add(message)) editor.value?.reset();
  } finally {
    busy.value = false;
  }
}

async function onUpdate(comment: CommentDto, message: string) {
  busy.value = true;
  try {
    await update(comment, message);
  } finally {
    busy.value = false;
  }
}

function onDeleteRequest(comment: CommentDto) {
  toDelete.value = comment;
  showDelete.value = true;
}

async function onDelete() {
  if (!toDelete.value) return;
  busy.value = true;
  try {
    await remove(toDelete.value);
  } finally {
    busy.value = false;
  }
}

watch(() => props.path, load, { immediate: true });
</script>
