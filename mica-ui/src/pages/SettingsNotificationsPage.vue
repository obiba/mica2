<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('settings.title')" to="/settings" />
        <q-breadcrumbs-el :label="t('settings.notifications')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <div class="text-h5 q-mb-sm">{{ t('config.notifications.title') }}</div>
      <p class="text-grey-8">{{ t('config.notifications.info') }}</p>
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <notifications-section
            :title="t('config.notifications.documents_title')"
            :help="t('config.notifications.documents_info')"
            :kinds="DOCUMENT_KINDS"
          />
        </div>
        <div class="col-12 col-md-6">
          <notifications-section
            :title="t('config.notifications.files_comments_title')"
            :help="t('config.notifications.files_comments_info')"
            :kinds="FILE_COMMENT_KINDS"
          />
          <contact-notifications-section />
        </div>
      </div>
    </q-page>
  </div>
</template>

<script setup lang="ts">
import NotificationsSection from 'src/components/settings/notifications/NotificationsSection.vue';
import ContactNotificationsSection from 'src/components/settings/notifications/ContactNotificationsSection.vue';
import { DOCUMENT_KINDS, FILE_COMMENT_KINDS } from 'src/components/settings/notifications/notifications';

const { t } = useI18n();
const systemStore = useSystemStore();

onMounted(() => {
  systemStore.init();
});
</script>
