<template>
  <config-section :title="t('config.content_access')" :items="items">
    <template #form="{ form }">
      <config-toggle v-model="form.openAccess" name="open_access" />
      <q-select
        v-model="form.summaryStatisticsAccessPolicy"
        :options="policyOptions"
        emit-value
        map-options
        dense
        outlined
        :label="t('config.summary_statistics_access_policy')"
        :hint="t('config.summary_statistics_access_policy_help')"
        class="q-mb-md"
      />
      <config-toggle v-model="form.signupEnabled" name="signup_enabled" />
      <config-toggle v-model="form.signupWithPassword" name="signup_with_password" />
      <q-select
        v-model="form.signupGroups"
        multiple
        use-input
        use-chips
        hide-dropdown-icon
        input-debounce="0"
        dense
        outlined
        :label="t('config.signup_groups')"
        :hint="t('config.signup_groups_form_help')"
        class="q-mb-md"
        @new-value="(value: string, done: (item?: string) => void) => onNewGroup(form, value, done)"
      />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem, formattedItem } from 'src/components/settings/general/fields';
import type { MicaConfigDto } from 'src/models/Mica';
import { SUMMARY_STATISTICS_ACCESS_POLICIES, splitGroups } from 'src/utils/config';

const { t } = useI18n();

const policyOptions = computed(() =>
  SUMMARY_STATISTICS_ACCESS_POLICIES.map((policy) => ({
    label: t(`config.summary_statistics_access_policy_label.${policy}`),
    value: policy,
  })),
);

/** several groups can be typed at once, space separated as in the legacy form */
function onNewGroup(form: MicaConfigDto, value: string, done: (item?: string) => void) {
  form.signupGroups = form.signupGroups || [];
  splitGroups(value).forEach((group) => {
    if (!form.signupGroups.includes(group)) form.signupGroups.push(group);
  });
  done();
}

const items = computed(() => [
  flagItem('openAccess', 'open_access'),
  formattedItem('summaryStatisticsAccessPolicy', 'summary_statistics_access_policy', (config: MicaConfigDto) =>
    config.summaryStatisticsAccessPolicy
      ? t(`config.summary_statistics_access_policy_label.${config.summaryStatisticsAccessPolicy}`)
      : '-',
  ),
  flagItem('signupEnabled', 'signup_enabled'),
  flagItem('signupWithPassword', 'signup_with_password'),
  formattedItem(
    'signupGroups',
    'signup_groups',
    (config: MicaConfigDto) => (config.signupGroups || []).join(', ') || '-',
  ),
]);
</script>
