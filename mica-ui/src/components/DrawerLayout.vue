<template>
  <q-layout view="hHh Lpr lff" container style="height: 100vh">
    <q-drawer
      v-model="drawer"
      show-if-above
      :mini="miniState"
      @mouseenter="miniState = false"
      @mouseleave="miniState = true"
      :width="width"
      :breakpoint="500"
      bordered
      class="bg-grey-3"
    >
      <slot name="drawer" />
    </q-drawer>

    <q-page-container>
      <q-page padding>
        <slot />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
interface Props {
  width?: number;
}

withDefaults(defineProps<Props>(), {
  width: 200,
});

const drawer = ref(false);
const miniState = ref(true);
</script>
