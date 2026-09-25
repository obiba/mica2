<template>
  <q-card-actions class="bg-grey-3 items-start">
    <q-input
      v-if="withComment"
      v-model="comment"
      class="col q-mr-md"
      dense
      outlined
      bg-color="white"
      :label="t('document.comment') + (commentRequired ? ' *' : '')"
      :hint="t('document.comment_hint')"
      :error="commentRequired && attempted && !comment"
      :error-message="t('required')"
    />
    <q-space v-else />
    <div class="text-no-wrap q-pt-xs">
      <q-btn flat :label="t('cancel')" color="secondary" :disable="saving" @click="emit('cancel')" />
      <q-btn :label="t('save')" color="primary" :loading="saving" class="q-ml-sm" @click="onSave" />
    </div>
  </q-card-actions>
</template>

<script setup lang="ts">
interface Props {
  /** an existing document is saved with a revision comment */
  withComment?: boolean;
  /** the configuration requires a comment on save */
  commentRequired?: boolean;
  saving?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ save: [comment: string | undefined]; cancel: [] }>();
const { t } = useI18n();

const comment = ref('');
const attempted = ref(false);

function onSave() {
  attempted.value = true;
  if (props.withComment && props.commentRequired && !comment.value) return;
  emit('save', props.withComment && comment.value ? comment.value : undefined);
}
</script>
