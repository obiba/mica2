<template>
  <div>
    <q-tab-panels v-model="tab">
      <q-tab-panel v-for="lang in languages" :key="lang" :name="lang" style="padding: 0">
        <q-input
          v-model="values[lang]"
          :label="label + (required ? ' *' : '')"
          :hint="hint"
          :type="rows && rows > 1 ? 'textarea' : 'text'"
          dense
          lazy-rules
          :disable="readonly"
          :rules="required ? [(val) => !!val || t('required')] : []"
          @update:model-value="onUpdate"
        />
      </q-tab-panel>
    </q-tab-panels>
    <div>
      <q-btn
        :flat="tab !== lang"
        dense
        size="sm"
        v-for="lang in languages"
        :key="lang"
        :label="lang"
        @click="tab = lang"
        :color="tab === lang ? 'primary' : 'grey-6'"
        class="q-mr-xs"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LocalizedStringDto } from 'src/models/Mica';

const systemStore = useSystemStore();
const { t } = useI18n();

interface Props {
  modelValue: LocalizedStringDto[] | undefined;
  label: string;
  hint?: string | undefined;
  required?: boolean | undefined;
  readonly?: boolean | undefined;
  rows?: number | undefined;
}

const props = defineProps<Props>();
const emits = defineEmits(['update:modelValue']);

const tab = ref('en');

const languages = computed(() => systemStore.configurationPublic.languages || ['en']);

const values = ref<Record<string, string>>({});

watch(
  () => props.modelValue,
  () => {
    languages.value.forEach((lang: string) => {
      const localizedString = props.modelValue?.find((x) => x.lang === lang);
      values.value[lang] = localizedString?.value || '';
    });
  },
  { immediate: true },
);

function onUpdate() {
  const localizedStrings = languages.value.map((lang: string) => ({ lang, value: values.value[lang] }));
  emits('update:modelValue', localizedStrings);
}
</script>
