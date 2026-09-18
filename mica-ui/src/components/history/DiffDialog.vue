<template>
  <q-dialog v-model="showDialog" @hide="onHide">
    <q-card class="dialog-lg" style="min-width: 60vw">
      <q-card-section>
        <div class="text-h6">{{ withPrevious ? t('history.diff_previous_title') : t('history.diff_current_title') }}</div>
        <div class="text-caption text-grey-7">
          <div>{{ t('history.left') }}: {{ describe(left) }}</div>
          <div>{{ t('history.right') }}: {{ describe(right) }}</div>
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section class="scroll" style="max-height: 60vh">
        <q-spinner-dots v-if="loading" color="primary" size="2em" />
        <div v-else-if="!diff || empty" class="text-grey-7">{{ t('history.diff_none') }}</div>
        <q-markup-table v-else flat dense wrap-cells>
          <thead>
            <tr>
              <th v-if="selectable"></th>
              <th class="text-left">{{ t('history.field') }}</th>
              <th class="text-left">{{ t('history.left') }}</th>
              <th class="text-left">{{ t('history.right') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.key">
              <td v-if="selectable">
                <q-checkbox dense :model-value="checked.has(row.key)" @update:model-value="toggle(row)" />
              </td>
              <td>
                <div class="text-weight-medium">{{ row.label || row.key }}</div>
                <div v-if="row.label" class="text-caption text-grey-7">{{ row.key }}</div>
              </td>
              <td>{{ row.left ?? '' }}</td>
              <td>
                <diff-text v-if="row.kind === 'differing'" :from="row.left" :to="row.right" />
                <span v-else>{{ row.right ?? '' }}</span>
              </td>
            </tr>
          </tbody>
        </q-markup-table>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn
          v-if="selectable && !empty"
          flat
          :label="checked.size > 0 ? `${t('history.restore')} (${checked.size})` : t('history.restore')"
          color="primary"
          v-close-popup
          @click="onRestore"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { GitCommitInfoDto } from 'src/models/Mica';
import DiffText from 'src/components/history/DiffText.vue';
import type { ChosenField, DocumentDiff } from 'src/utils/restoreFields';
import { diffSize } from 'src/utils/restoreFields';
import { getDateLabel } from 'src/utils/dates';

interface Props {
  modelValue: boolean;
  /** the newer revision (the current one when restoring) */
  left: GitCommitInfoDto | undefined;
  /** the older revision */
  right: GitCommitInfoDto | undefined;
  diff: DocumentDiff | undefined;
  loading?: boolean;
  /** the left commit is compared with the one before it: nothing to restore */
  withPrevious?: boolean;
  canRestore?: boolean;
}

interface DiffRow {
  key: string;
  kind: 'onlyLeft' | 'differing' | 'onlyRight';
  label: string | undefined;
  left: unknown;
  right: unknown;
  /** what restoring this field means: remove it, or set the older value */
  chosen: ChosenField;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  /** an empty list restores the whole revision */
  restore: [chosen: ChosenField[]];
}>();
const { t } = useI18n();

const showDialog = ref(props.modelValue);
const checked = ref(new Map<string, ChosenField>());

watch(
  () => props.modelValue,
  (value) => {
    showDialog.value = value;
    if (value) checked.value = new Map();
  },
);

const selectable = computed(() => props.canRestore === true && !props.withPrevious);
const empty = computed(() => !props.diff || diffSize(props.diff) === 0);

const rows = computed<DiffRow[]>(() => {
  if (!props.diff) return [];
  const label = (values: unknown[]) => (typeof values[0] === 'string' ? values[0] : undefined);
  return [
    ...Object.entries(props.diff.onlyLeft).map(
      ([key, values]): DiffRow => ({ key, kind: 'onlyLeft', label: label(values), left: values[1], right: undefined, chosen: key }),
    ),
    ...Object.entries(props.diff.differing).map(
      ([key, values]): DiffRow => ({ key, kind: 'differing', label: label(values), left: values[1], right: values[2], chosen: { name: key, value: values[2] } }),
    ),
    ...Object.entries(props.diff.onlyRight).map(
      ([key, values]): DiffRow => ({ key, kind: 'onlyRight', label: label(values), left: undefined, right: values[1], chosen: { name: key, value: values[1] } }),
    ),
  ];
});

function describe(commit: GitCommitInfoDto | undefined) {
  return commit ? `${commit.author}, ${getDateLabel(commit.date)}` : '';
}

function toggle(row: DiffRow) {
  const next = new Map(checked.value);
  if (next.has(row.key)) {
    next.delete(row.key);
  } else {
    next.set(row.key, row.chosen);
  }
  checked.value = next;
}

function onRestore() {
  // all the fields, or none: the whole revision
  const chosen = checked.value.size > 0 && checked.value.size < rows.value.length ? [...checked.value.values()] : [];
  emit('restore', chosen);
}

function onHide() {
  emit('update:modelValue', false);
}
</script>
