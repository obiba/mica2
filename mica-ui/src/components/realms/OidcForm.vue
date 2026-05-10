<template>
  <div>
    <div class="text-bold">{{ t('realm.oidc.title') }}</div>
    <q-input
      v-model="config.clientId"
      :label="t('realm.oidc.client_id') + ' *'"
      :hint="t('realm.oidc.client_id_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.secret"
      :label="t('realm.oidc.client_secret') + ' *'"
      :hint="t('realm.oidc.client_secret_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.discoveryURI"
      :label="t('realm.oidc.discovery_uri') + ' *'"
      :hint="t('realm.oidc.discovery_uri_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.providerUrl"
      :label="t('realm.oidc.account_url')"
      :hint="t('realm.oidc.account_url_hint')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.scope"
      :label="t('realm.oidc.scope')"
      :hint="t('realm.oidc.scope_hint')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.groupsClaim"
      :label="t('realm.oidc.groups_claim')"
      :hint="t('realm.oidc.groups_claim_hint')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.groupsJS"
      :label="t('realm.oidc.groups_js')"
      :hint="t('realm.oidc.groups_js_hint')"
      :placeholder="groupsJSPlaceholder"
      type="textarea"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.prompt"
      :label="t('realm.oidc.prompt')"
      :hint="t('realm.oidc.prompt_hint')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model.number="config.maxAge"
      :label="t('realm.oidc.max_age')"
      :hint="t('realm.oidc.max_age_hint')"
      type="number"
      :min="0"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-toggle
      v-model="config.useNonce"
      :label="t('realm.oidc.nonce')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <div class="row q-col-gutter-md q-mb-md">
      <div class="col-12 col-sm-6">
        <q-input
          v-model.number="config.connectTimeout"
          :label="t('realm.oidc.connect_timeout')"
          :hint="t('realm.oidc.connect_timeout_hint')"
          type="number"
          :min="0"
          dense
          class="q-mb-md"
          @update:model-value="onUpdate"
        />
      </div>
      <div class="col-12 col-sm-6">
        <q-input
          v-model.number="config.readTimeout"
          :label="t('realm.oidc.read_timeout')"
          :hint="t('realm.oidc.read_timeout_hint')"
          type="number"
          :min="0"
          dense
          class="q-mb-md"
          @update:model-value="onUpdate"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { OIDCRealmConfig } from 'src/components/models';

const { t } = useI18n();

interface Props {
  modelValue: string | undefined;
}

const DefaultConfig = {
  clientId: '',
  secret: '',
  discoveryURI: '',
  providerUrl: '',
  scope: 'openid',
  groupsClaim: '',
  groupsJS: '',
  useNonce: true,
  connectTimeout: 0,
  readTimeout: 0,
  prompt: undefined,
  maxAge: undefined,
};

const groupsJSPlaceholder = `// input: userInfo
// output: string or array of strings
// example:
userInfo.some.property.map(x => x.split (':')[0])`;

const props = defineProps<Props>();
const emits = defineEmits(['update:modelValue']);

const config = ref<OIDCRealmConfig>(props.modelValue ? JSON.parse(props.modelValue) : DefaultConfig);

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
  if (config.value.maxAge && isNaN(config.value.maxAge)) {
    config.value.maxAge = undefined;
  }
  emits('update:modelValue', JSON.stringify(config.value));
}
</script>
