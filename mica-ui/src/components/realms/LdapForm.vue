<template>
  <div>
    <div class="text-bold">{{ t('realm.ldap.title') }}</div>
    <q-input
      v-model="config.url"
      :label="t('realm.ldap.url') + ' *'"
      :hint="t('realm.ldap.url')"
      placeholder="ldap://example.org:389"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.systemUsername"
      :label="t('realm.ldap.system_username') + ' *'"
      :hint="t('realm.ldap.system_username_hint')"
      autocomplete="nope"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.systemPassword"
      :label="t('realm.ldap.system_password') + ' *'"
      :hint="t('realm.ldap.system_password_hint')"
      type="password"
      autocomplete="new-password"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.userDnTemplate"
      :label="t('realm.ldap.user_dn_template') + ' *'"
      :hint="t('realm.ldap.user_dn_template_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      @update:model-value="onUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import type { LDAPRealmConfig } from 'src/components/models';

const { t } = useI18n();

interface Props {
  modelValue: string | undefined;
}

const DefaultConfig = {
  url: '',
  systemUsername: '',
  systemPassword: '',
  userDnTemplate: '',
};

const props = defineProps<Props>();
const emits = defineEmits(['update:modelValue']);

const config = ref<LDAPRealmConfig>(props.modelValue ? JSON.parse(props.modelValue) : DefaultConfig);

watch(
  () => props.modelValue,
  () => {
    try {
      config.value = props.modelValue ? JSON.parse(props.modelValue) : DefaultConfig;
    } catch (e) {
      console.error(e);
      config.value = DefaultConfig;
    }
  },
  { immediate: true },
);

function onUpdate() {
  emits('update:modelValue', JSON.stringify(config.value));
}
</script>
