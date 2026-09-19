<template>
  <div>
    <div class="row items-end">
      <q-tabs
        v-model="mode"
        dense
        no-caps
        inline-label
        align="left"
        class="col text-grey"
        active-color="primary"
        indicator-color="primary"
      >
        <q-tab name="write" icon="edit_note" :label="t('comments.write')" />
        <q-tab name="preview" icon="visibility" :label="t('comments.preview')" :disable="!message.trim()" />
      </q-tabs>
      <a
        href="https://guides.github.com/features/mastering-markdown/"
        target="_blank"
        rel="noopener"
        class="col-auto text-caption q-mb-xs"
      >
        {{ t('comments.markdown_doc') }}
        <q-icon name="open_in_new" />
      </a>
    </div>
    <q-separator />
    <q-input
      v-if="mode === 'write'"
      v-model="message"
      type="textarea"
      rows="5"
      outlined
      dense
      :placeholder="t('comments.placeholder')"
      :disable="saving"
      input-class="comment-editor-input"
      class="q-mt-sm"
    />
    <markdown-text v-else :text="message" class="q-pa-sm q-mt-sm comment-editor-preview" />
    <div class="row q-gutter-sm q-mt-xs">
      <q-btn
        color="primary"
        size="sm"
        :label="label ?? t('comments.send')"
        :disable="!message.trim() || message === modelValue"
        :loading="saving"
        @click="onSubmit"
      />
      <q-btn v-if="cancellable" flat color="secondary" size="sm" :label="t('cancel')" @click="onCancel" />
    </div>
  </div>
</template>

<script setup lang="ts">
import MarkdownText from 'src/components/comments/MarkdownText.vue';

interface Props {
  /** the message being edited, empty for a new comment */
  modelValue?: string;
  /** label of the submit button, "Comment" by default */
  label?: string;
  /** show a cancel button (editing an existing comment) */
  cancellable?: boolean;
  saving?: boolean;
}

const props = withDefaults(defineProps<Props>(), { modelValue: '' });
const emit = defineEmits<{ submit: [message: string]; cancel: [] }>();
const { t } = useI18n();

const mode = ref<'write' | 'preview'>('write');
const message = ref(props.modelValue);

watch(
  () => props.modelValue,
  (value) => {
    message.value = value;
    mode.value = 'write';
  },
);

function onSubmit() {
  emit('submit', message.value.trim());
}

function onCancel() {
  message.value = props.modelValue;
  mode.value = 'write';
  emit('cancel');
}

/** empties the editor once a new comment is posted */
function reset() {
  message.value = '';
  mode.value = 'write';
}

defineExpose({ reset });
</script>

<style lang="scss" scoped>
:deep(.comment-editor-input) {
  min-height: 5em;
}
.comment-editor-preview {
  min-height: 5em;
  border: 1px solid rgba(0, 0, 0, 0.24);
  border-radius: 4px;
}
</style>
