<template>
  <div>
    <div class="text-bold">{{ t('realm.mappings.title') }}</div>
    <div class="text-hint q-mb-md">{{ t('realm.mappings.hint') }}</div>
    <q-input
      v-model="username"
      :label="t('realm.mappings.username') + ' *'"
      :hint="t('realm.mappings.username_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="email"
      :label="t('realm.mappings.email') + ' *'"
      :hint="t('realm.mappings.email_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="firstname"
      :label="t('realm.mappings.firstname') + ' *'"
      :hint="t('realm.mappings.firstname_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-input
      v-model="lastname"
      :label="t('realm.mappings.lastname') + ' *'"
      :hint="t('realm.mappings.lastname_hint')"
      dense
      lazy-rules
      :rules="[(val) => !!val || t('required')]"
      class="q-mb-md"
      @update:model-value="onUpdate"
    />
    <q-list>
      <q-item v-for="(entry, idx) in extras" :key="idx" class="q-pa-none">
        <q-item-section>
          <div class="row q-col-gutter-md">
            <div class="col">
              <q-input
                v-model="entry.key"
                :label="t('realm.mappings.agate_key')"
                dense
                lazy-rules
                :rules="[(val) => !!val || t('required')]"
                @update:model-value="onUpdate"
              />
            </div>
            <div class="col">
              <q-input
                v-model="entry.value"
                :label="t('realm.mappings.provider_key')"
                dense
                lazy-rules
                :rules="[(val) => !!val || t('required')]"
                @update:model-value="onUpdate"
              />
            </div>
          </div>
        </q-item-section>
        <q-item-section side>
          <q-btn flat icon="delete" color="accent" size="sm" @click="onEntryDelete(idx)" />
        </q-item-section>
      </q-item>
    </q-list>
    <q-btn color="primary" icon="add" size="sm" @click="extras.push({ key: '', value: '' })" />
  </div>
</template>

<script setup lang="ts">
import type { RealmConfigDto_UserInfoMappingDto } from 'src/models/Mica';
const { t } = useI18n();

interface Props {
  modelValue: RealmConfigDto_UserInfoMappingDto[] | undefined;
}

const props = defineProps<Props>();
const emits = defineEmits(['update:modelValue']);

const username = ref('preferred_username');
const email = ref('email');
const firstname = ref('given_name');
const lastname = ref('family_name');
const extras = ref<RealmConfigDto_UserInfoMappingDto[]>([]);

watch(
  () => props.modelValue,
  () => {
    username.value = props.modelValue?.find((m) => m.key === 'username')?.value || 'preferred_username';
    email.value = props.modelValue?.find((m) => m.key === 'email')?.value || 'email';
    firstname.value = props.modelValue?.find((m) => m.key === 'firstname')?.value || 'given_name';
    lastname.value = props.modelValue?.find((m) => m.key === 'lastname')?.value || 'family_name';
    extras.value = JSON.parse(
      JSON.stringify(
        props.modelValue?.filter((m) => !['username', 'email', 'firstname', 'lastname'].includes(m.key)) || [],
      ),
    );
  },
  { immediate: true },
);

function onUpdate() {
  const mappings = [
    { key: 'username', value: username.value },
    { key: 'email', value: email.value },
    { key: 'firstname', value: firstname.value },
    { key: 'lastname', value: lastname.value },
    ...extras.value,
  ];
  emits('update:modelValue', mappings);
}

function onEntryDelete(idx: number) {
  extras.value.splice(idx, 1);
  onUpdate();
}
</script>
