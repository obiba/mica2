<template>
  <div>
    <div class="text-bold">{{ t('realm.ad.title') }}</div>
    <q-input
      v-model="config.url"
      :label="t('realm.ad.url') + ' *'"
      :hint="t('realm.ad.url')"
      placeholder="ldap://example.org:389"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.systemUsername"
      :label="t('realm.ad.system_username') + ' *'"
      :hint="t('realm.ad.system_username_hint')"
      autocomplete="nope"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.systemPassword"
      :label="t('realm.ad.system_password') + ' *'"
      :hint="t('realm.ad.system_password_hint')"
      type="password"
      autocomplete="new-password"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.searchFilter"
      :label="t('realm.ad.search_filter') + ' *'"
      :hint="t('realm.ad.search_filter_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.searchBase"
      :label="t('realm.ad.search_base')"
      :hint="t('realm.ad.search_base_hint')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.principalSuffix"
      :label="t('realm.ad.principal_suffix')"
      :hint="t('realm.ad.principal_suffix_hint')"
      dense
      @update:model-value="onUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import type { ADRealmConfig } from 'src/components/models';

const { t } = useI18n();

interface Props {
  modelValue: string | undefined;
}

const DefaultConfig = {
  url: '',
  systemUsername: '',
  systemPassword: '',
  searchFilter: '',
  searchBase: '',
  principalSuffix: '',
};

const props = defineProps<Props>();
const emits = defineEmits(['update:modelValue']);

const config = ref<ADRealmConfig>(props.modelValue ? JSON.parse(props.modelValue) : DefaultConfig);

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
