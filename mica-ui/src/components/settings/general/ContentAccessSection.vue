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
      <groups-select
        v-model="form.signupGroups"
        :label="t('config.signup_groups')"
        :hint="t('config.signup_groups_form_help')"
      />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import GroupsSelect from 'src/components/settings/general/GroupsSelect.vue';
import { flagItem, formattedItem } from 'src/components/settings/general/fields';
import type { MicaConfigDto } from 'src/models/Mica';
import { SUMMARY_STATISTICS_ACCESS_POLICIES } from 'src/utils/config';

const { t } = useI18n();

const policyOptions = computed(() =>
  SUMMARY_STATISTICS_ACCESS_POLICIES.map((policy) => ({
    label: t(`config.summary_statistics_access_policy_label.${policy}`),
    value: policy,
  })),
);

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
