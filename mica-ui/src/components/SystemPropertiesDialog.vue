<template>
  <q-dialog v-model="showDialog" persistent @hide="onHide">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ t('properties') }}</div>
      </q-card-section>

      <q-separator />

      <q-card-section>
        <q-form>
          <q-input
            v-model="configuration.name"
            :label="t('name')"
            :hint="t('system.name_hint')"
            dense
            class="q-mb-md"
          />
          <q-input
            v-model="configuration.publicUrl"
            :label="t('system.public_url')"
            :hint="t('system.public_url_hint')"
            dense
            class="q-mb-md"
          />
          <div class="row q-col-gutter-md q-mb-md">
            <div class="col-12 col-sm-6">
              <q-input
                v-model="configuration.portalUrl"
                :label="t('system.portal_url')"
                :hint="t('system.portal_url_hint')"
                dense
                class="q-mb-md"
              />
            </div>
            <div class="col-12 col-sm-6">
              <q-input
                v-model="configuration.domain"
                :label="t('system.sso_domain')"
                :hint="t('system.sso_domain_hint')"
                dense
                class="q-mb-md"
              />
            </div>
          </div>
          <div class="row q-col-gutter-md q-mb-md">
            <div class="col-12 col-sm-4">
              <q-input
                v-model.number="configuration.shortTimeout"
                type="number"
                :label="t('system.short_timeout')"
                :hint="t('system.short_timeout_hint')"
                dense
                min="-1"
                lazy-rules
                :rules="[(val) => (typeof val === 'number' && val >= -1) || t('number_invalid')]"
                class="q-mb-md"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-input
                v-model.number="configuration.longTimeout"
                type="number"
                :label="t('system.long_timeout')"
                :hint="t('system.long_timeout_hint')"
                dense
                min="-1"
                lazy-rules
                :rules="[(val) => (typeof val === 'number' && val >= -1) || t('number_invalid')]"
                class="q-mb-md"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-input
                v-model.number="configuration.inactiveTimeout"
                type="number"
                :label="t('system.inactive_timeout')"
                :hint="t('system.inactive_timeout_hint')"
                dense
                min="-1"
                lazy-rules
                :rules="[(val) => (typeof val === 'number' && val >= -1) || t('number_invalid')]"
                class="q-mb-md"
              />
            </div>
          </div>
          <div class="row q-col-gutter-md q-mb-md">
            <div class="col-12 col-sm-6">
              <q-checkbox
                v-model="configuration.joinPageEnabled"
                :label="t('system.signup_enabled')"
                dense
                class="q-mb-xs"
              />
              <div class="text-hint">{{ t('system.signup_enabled_hint') }}</div>
            </div>
            <div class="col-12 col-sm-6">
              <q-checkbox
                v-model="configuration.joinWithUsername"
                :label="t('system.signup_username')"
                dense
                class="q-mb-xs"
              />
              <div class="text-hint">{{ t('system.signup_username_hint') }}</div>
            </div>
          </div>
          <div class="row q-col-gutter-md q-mb-md">
            <div class="col-12 col-sm-6">
              <q-input
                v-model="configuration.joinWhitelist"
                :label="t('system.signup_whitelist')"
                :hint="t('system.signup_whitelist_hint_form')"
                dense
                class="q-mb-md"
              />
            </div>
            <div class="col-12 col-sm-6">
              <q-input
                v-model="configuration.joinBlacklist"
                :label="t('system.signup_blacklist')"
                :hint="t('system.signup_blacklist_hint_form')"
                dense
                class="q-mb-md"
              />
            </div>
          </div>
          <div class="row q-col-gutter-md q-mb-md">
            <div class="col-12 col-sm-6">
              <q-select
                v-model="configuration.languages"
                :label="t('system.languages')"
                :hint="t('system.languages_hint')"
                dense
                use-input
                use-chips
                multiple
                hide-dropdown-icon
                input-debounce="0"
                new-value-mode="add-unique"
              />
            </div>
            <div class="col-12 col-sm-6">
              <q-select
                v-model="configuration.enforced2FAStrategy"
                :label="t('system.otp_strategy')"
                :hint="t('system.otp_strategy_hint')"
                :options="otpOptions"
                dense
                emit-value
                map-options
              />
            </div>
          </div>
        </q-form>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" @click="onCancel" v-close-popup />
        <q-btn flat :label="t('save')" :disable="!isValid" color="primary" @click="onSave" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { MicaConfigDto } from 'src/models/Mica';
import { notifyError, notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const systemStore = useSystemStore();

interface DialogProps {
  modelValue: boolean;
}

const props = defineProps<DialogProps>();
const emit = defineEmits(['update:modelValue', 'saved', 'cancel']);

const showDialog = ref(props.modelValue);
const configuration = ref<MicaConfigDto>({ ...systemStore.configuration });
const otpOptions = computed(() =>
  ['NONE', 'APP', 'ANY'].map((value) => ({ label: t(`system.otp_strategies.${value}`), value })),
);
const isValid = computed(
  () =>
    typeof configuration.value.shortTimeout === 'number' &&
    typeof configuration.value.longTimeout === 'number' &&
    typeof configuration.value.inactiveTimeout === 'number',
);

watch(
  () => props.modelValue,
  (value) => {
    showDialog.value = value;
    configuration.value = { ...systemStore.configuration };
  },
);

function onHide() {
  emit('update:modelValue', false);
}

function onCancel() {
  emit('cancel');
}

function onSave() {
  systemStore
    .save(configuration.value)
    .then(() => {
      notifySuccess(t('system.properties_updated'));
      emit('saved');
    })
    .catch(() => {
      notifyError(t('system.properties_update_failed'));
    })
    .finally(() => {
      emit('update:modelValue', false);
    });
}
</script>
