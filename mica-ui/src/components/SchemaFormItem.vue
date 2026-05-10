<template>
  <div>
    <div v-if="isPassword()">
      <q-input
        v-model="dataString"
        :label="field.title"
        :hint="field.description"
        type="password"
        autocomplete="new-password"
        dense
        :disable="disable"
        class="q-mb-md"
        :debounce="500"
        @update:model-value="onUpdate"
      />
    </div>
    <div v-else-if="isString()">
      <q-select
        v-if="field.enum"
        v-model="dataString"
        :label="field.title"
        :hint="field.description"
        dense
        :disable="disable"
        class="q-mb-md"
        emit-value
        map-options
        :options="field.enum.map((opt: EnumOption) => ({ label: opt.title, value: opt.key }))"
        @update:model-value="onUpdate"
      />
      <q-input
        v-else
        v-model="dataString"
        :label="field.title"
        :hint="field.description"
        autocomplete="off"
        dense
        :disable="disable"
        class="q-mb-md"
        :debounce="500"
        @update:model-value="onUpdate"
      />
    </div>
    <div v-else-if="isNumber()">
      <q-input
        v-model.number="dataNumber"
        :label="field.title"
        :hint="field.description"
        type="number"
        dense
        :disable="disable"
        class="q-mb-md"
        :debounce="500"
        @update:model-value="onUpdate"
      />
    </div>
    <div v-else-if="isInteger()">
      <q-input
        v-model.number="dataInteger"
        :label="field.title"
        :hint="field.description"
        type="number"
        dense
        :disable="disable"
        class="q-mb-md"
        :debounce="500"
        @update:model-value="onUpdate"
      />
    </div>
    <div v-else-if="isBoolean()">
      <q-toggle
        v-model="dataBoolean"
        :label="field.title"
        :hint="field.description"
        dense
        :disable="disable"
        :class="field.description ? 'q-mb-xs' : 'q-mb-md'"
        @update:model-value="onUpdate"
      />
      <div v-if="field.description" class="text-hint q-mb-md">
        {{ field.description }}
      </div>
    </div>
    <div v-else-if="field.type === 'array'">
      <div class="text-help">{{ field.title }}</div>
      <div class="text-hint q-mb-sm">{{ field.description }}</div>
      <q-list separator bordered>
        <q-item v-for="(datum, index) in dataArray" :key="index">
          <q-item-section>
            <div v-for="item in field.items" :key="item.key">
              <schema-form-item
                v-if="dataArray[index] !== undefined"
                v-model="dataArray[index][item.key]"
                :field="item"
                :disable="disable"
                @update:model-value="onArrayUpdate(index)"
              />
            </div>
          </q-item-section>
          <q-item-section v-if="!disable" avatar>
            <q-btn
              rounded
              dense
              flat
              size="sm"
              color="secondary"
              :title="t('delete')"
              icon="delete"
              @click="dataArray.splice(index, 1)"
            />
          </q-item-section>
        </q-item>
      </q-list>
      <q-btn
        v-if="!disable"
        color="primary"
        icon="add"
        :label="t('add')"
        @click="dataArray.push({})"
        size="sm"
        class="q-mt-sm"
      />
    </div>
    <div v-else>
      {{ logUnsupported() }}
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FileObject, FormObject, SchemaFormField, EnumOption } from 'src/components/models';

const { t } = useI18n();

interface Props {
  modelValue: string | number | boolean | FileObject | FormObject | Array<FormObject> | undefined;
  field: SchemaFormField;
  disable?: boolean | undefined;
}

const props = defineProps<Props>();
const emit = defineEmits(['update:modelValue']);

const data = ref(props.modelValue);

const dataString = ref('');
const dataNumber = ref<number>();
const dataInteger = ref<number>();
const dataBoolean = ref<boolean>();
const dataArray = ref<Array<FormObject>>([]);

//onMounted(init);

watch([() => props.modelValue, () => props.field], init, { immediate: true });

function init() {
  data.value = props.modelValue;
  if (isArray()) {
    dataArray.value = data.value ? (data.value as Array<FormObject>) : [];
  } else if (isString()) {
    dataString.value = data.value as string;
  } else if (isNumber()) {
    dataNumber.value = data.value as number;
  } else if (isInteger()) {
    dataInteger.value = data.value as number;
  } else if (isBoolean()) {
    dataBoolean.value = data.value === true || data.value === 'true';
  }
}

function isString() {
  return props.field.type === 'string';
}

function isPassword() {
  return isString() && props.field.format === 'password';
}

function isArray() {
  return props.field.type === 'array';
}

function isNumber() {
  return props.field.type === 'number';
}

function isInteger() {
  return props.field.type === 'integer';
}

function isBoolean() {
  return props.field.type === 'boolean';
}

function onUpdate() {
  if (isArray()) {
    data.value = dataArray.value;
  } else if (isString()) {
    data.value = dataString.value;
  } else if (isNumber()) {
    data.value = dataNumber.value;
  } else if (isInteger()) {
    if (dataInteger.value) {
      // Remove decimal part
      data.value = dataInteger.value = Math.floor(dataInteger.value);
    } else {
      data.value = undefined;
    }
  } else if (isBoolean()) {
    data.value = dataBoolean.value;
  }
  emit('update:modelValue', data.value);
}

function onArrayUpdate(index: number) {
  onUpdate();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (value: any) => {
    dataArray.value[index] = value;
  };
}

function logUnsupported() {
  console.error('Unsupported form item', props.field);
}
</script>
