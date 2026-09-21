<template>
  <q-select
    v-model="model"
    multiple
    use-input
    use-chips
    hide-dropdown-icon
    input-debounce="0"
    dense
    outlined
    :label="label"
    :hint="hint"
    class="q-mb-md"
    @new-value="onNewValue"
    @input-value="(value: string) => (pending = value)"
    @blur="onBlur"
  />
</template>

<script setup lang="ts">
import { splitGroups } from 'src/utils/config';

interface Props {
  label: string;
  hint?: string;
}

defineProps<Props>();
/** the group names, as chips */
const model = defineModel<string[] | undefined>();

/** the groups typed but not yet committed with Enter */
const pending = ref('');

/** several groups can be typed at once, space separated as in the legacy form */
function onNewValue(value: string, done: (item?: string) => void) {
  const groups = [...(model.value || [])];
  splitGroups(value).forEach((group) => {
    if (!groups.includes(group)) groups.push(group);
  });
  model.value = groups;
  done();
}

/** groups typed without Enter are still saved when leaving the field (as in the legacy text input) */
function onBlur() {
  if (!pending.value.trim()) return;
  onNewValue(pending.value, () => {
    pending.value = '';
  });
}
</script>
