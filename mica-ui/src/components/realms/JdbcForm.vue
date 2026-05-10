<template>
  <div>
    <div class="text-bold">{{ t('realm.jdbc.title') }}</div>
    <q-input
      v-model="config.url"
      :label="t('realm.jdbc.url') + ' *'"
      :hint="t('realm.jdbc.url')"
      placeholder="jdbc:mysql://example.org:3306/users_db"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.username"
      :label="t('realm.jdbc.username') + ' *'"
      :hint="t('realm.jdbc.username_hint')"
      autocomplete="nope"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.password"
      :label="t('realm.jdbc.password') + ' *'"
      :hint="t('realm.jdbc.password_hint')"
      type="password"
      autocomplete="new-password"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="config.authenticationQuery"
      :label="t('realm.jdbc.auth_query') + ' *'"
      :hint="t('realm.jdbc.auth_query_hint')"
      placeholder="SELECT password FROM users WHERE username = ?"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <div v-if="config.saltStyle === 'COLUMN'" class="box-warning text-hint q-mb-md">
      {{ t('realm.jdbc.auth_query_salt_column_hint') }}
    </div>
    <q-select
      v-model="config.saltStyle"
      :label="t('realm.jdbc.salt_style')"
      :hint="t('realm.jdbc.salt_style_hint')"
      :options="saltOptions"
      dense
      emit-value
      map-options
      lazy-rules
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-if="config.saltStyle === 'EXTERNAL'"
      v-model="config.externalSalt"
      :label="t('realm.jdbc.external_salt')"
      :hint="t('realm.jdbc.external_salt_hint')"
      dense
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-if="config.saltStyle === 'COLUMN' || config.saltStyle === 'EXTERNAL'"
      v-model="config.algorithmName"
      :label="t('realm.jdbc.algorithm_name')"
      :hint="t('realm.jdbc.algorithm_name_hint')"
      placeholder="SHA-256"
      dense
      @update:model-value="onUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import type { JDBCRealmConfig } from 'src/components/models';

const { t } = useI18n();

interface Props {
  modelValue: string | undefined;
}

const DefaultConfig = {
  url: '',
  username: '',
  password: '',
  authenticationQuery: '',
  saltStyle: 'NO_SALT',
};
const saltOptions = ['NO_SALT', 'CRYPT', 'COLUMN', 'EXTERNAL'];

const props = defineProps<Props>();
const emits = defineEmits(['update:modelValue']);

const config = ref<JDBCRealmConfig>(props.modelValue ? JSON.parse(props.modelValue) : DefaultConfig);

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
  if (config.value.saltStyle !== 'EXTERNAL') {
    delete config.value.externalSalt;
    if (config.value.saltStyle !== 'COLUMN') {
      delete config.value.algorithmName;
    }
  }
  emits('update:modelValue', JSON.stringify(config.value));
}
</script>
