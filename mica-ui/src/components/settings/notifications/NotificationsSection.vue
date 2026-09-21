<template>
  <config-section :title="title" :help="help" :items="items">
    <template #form="{ form }">
      <template v-for="kind in kinds" :key="kind.key">
        <config-toggle v-model="form[kind.enabledField]" :name="`notifications.${kind.key}`" />
        <config-input
          v-model="form[kind.subjectField]"
          :name="`notifications.${kind.key}_subject`"
          :hint="subjectHint(kind)"
          :placeholder="kind.placeholder"
          :disable="!form[kind.enabledField]"
        />
      </template>
      <slot name="form" :form="form" />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { notificationItem } from 'src/components/settings/notifications/notifications';
import type { NotificationKind } from 'src/components/settings/notifications/notifications';
import type { FieldItem } from 'src/components/FieldsList.vue';
import type { MicaConfigDto } from 'src/models/Mica';

interface Props {
  title: string;
  help: string;
  /** the notifications of the card: a toggle and a subject each */
  kinds: NotificationKind[];
  /** the fields shown after the ones of the kinds */
  extraItems?: FieldItem[];
}

const props = defineProps<Props>();
defineSlots<{
  /** the fields edited after the ones of the kinds, bound to the working copy of the configuration */
  form(props: { form: MicaConfigDto }): unknown;
}>();

const { t } = useI18n();

/** the reserved terms the subject can contain */
function subjectHint(kind: NotificationKind): string {
  return t(`config.notifications.${kind.key}_subject_help`, { terms: kind.terms.join(', ') });
}

/** one row per kind: whether it is enabled, and the subject (the default one when not set) */
const items = computed<FieldItem[]>(() => [...props.kinds.map(notificationItem), ...(props.extraItems ?? [])]);
</script>
