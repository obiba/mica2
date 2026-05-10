<template>
  <div>
    <q-btn @click="onEdit" label="Edit" icon="edit" size="sm" color="primary" class="q-mb-md" />
    <div class="row q-col-gutter-md">
      <div class="col-md-6 col-sm-12">
        <q-list separator class="fields-list">
          <q-item v-ripple>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('name') }}</q-item-label>
              <q-item-label caption>{{ t('system.name_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.name }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.public_url') }}</q-item-label>
              <q-item-label caption>{{ t('system.public_url_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              <a v-if="config.publicUrl" :href="config.publicUrl" target="_blank">{{ config.publicUrl }}</a>
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.portal_url') }}</q-item-label>
              <q-item-label caption>{{ t('system.portal_url_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              <a v-if="config.portalUrl" :href="config.portalUrl" target="_blank">{{ config.portalUrl }}</a>
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.sso_domain') }}</q-item-label>
              <q-item-label caption>{{ t('system.sso_domain_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.domain }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.short_timeout') }}</q-item-label>
              <q-item-label caption>{{ t('system.short_timeout_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.shortTimeout }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.long_timeout') }}</q-item-label>
              <q-item-label caption>{{ t('system.long_timeout_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.longTimeout }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.inactive_timeout') }}</q-item-label>
              <q-item-label caption>{{ t('system.inactive_timeout_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.inactiveTimeout }}
            </q-item-section>
          </q-item>
        </q-list>
      </div>
      <div class="col-md-6 col-sm-12">
        <q-list separator class="fields-list">
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.languages') }}</q-item-label>
              <q-item-label caption>{{ t('system.languages_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.languages?.join(', ') }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.signup_enabled') }}</q-item-label>
              <q-item-label caption>{{ t('system.signup_enabled_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              <q-icon :name="config.joinPageEnabled ? 'check_box' : 'check_box_outline_blank'" size="sm" dense />
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.signup_username') }}</q-item-label>
              <q-item-label caption>{{ t('system.signup_username_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              <q-icon :name="config.joinWithUsername ? 'check_box' : 'check_box_outline_blank'" size="sm" dense />
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.signup_whitelist') }}</q-item-label>
              <q-item-label caption>{{ t('system.signup_whitelist_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.joinWhitelist }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.signup_blacklist') }}</q-item-label>
              <q-item-label caption>{{ t('system.signup_blacklist_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.joinBlacklist }}
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section>
              <q-item-label class="text-overline">{{ t('system.otp_strategy') }}</q-item-label>
              <q-item-label caption>{{ t('system.otp_strategy_hint') }}</q-item-label>
            </q-item-section>
            <q-item-section avatar>
              {{ config.enforced2FAStrategy ? t(`system.otp_strategies.${config.enforced2FAStrategy}`) : '' }}
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </div>
    <system-properties-dialog v-model="showDialog" @saved="onSaved" />
  </div>
</template>

<script setup lang="ts">
import SystemPropertiesDialog from 'src/components/SystemPropertiesDialog.vue';
const systemStore = useSystemStore();
const { t } = useI18n();

const showDialog = ref(false);
const config = computed(() => systemStore.configuration);

const onEdit = () => {
  showDialog.value = true;
};

const onSaved = () => {
  showDialog.value = false;
  systemStore.init();
};
</script>
