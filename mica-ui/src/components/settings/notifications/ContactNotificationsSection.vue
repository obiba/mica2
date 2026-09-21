<template>
  <notifications-section
    :title="t('config.notifications.contact_title')"
    :help="t('config.notifications.contact_info')"
    :kinds="[CONTACT_KIND]"
    :extra-items="items"
  >
    <template #form="{ form }">
      <groups-select
        v-model="form.contactGroups"
        :label="t('config.notifications.contact_groups')"
        :hint="t('config.notifications.contact_groups_form_help')"
      />
    </template>
  </notifications-section>
</template>

<script setup lang="ts">
import NotificationsSection from 'src/components/settings/notifications/NotificationsSection.vue';
import GroupsSelect from 'src/components/settings/general/GroupsSelect.vue';
import { formattedItem } from 'src/components/settings/general/fields';
import { CONTACT_KIND } from 'src/components/settings/notifications/notifications';
import type { MicaConfigDto } from 'src/models/Mica';

const { t } = useI18n();

const items = [
  formattedItem(
    'contactGroups',
    'notifications.contact_groups',
    (config: MicaConfigDto) => (config.contactGroups || []).join(', ') || '-',
  ),
];
</script>
