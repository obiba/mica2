<template>
  <div class="drawer-layout row no-wrap">
    <aside
      class="drawer-layout__aside bg-grey-3 print-hide"
      :class="{ 'drawer-layout__aside--mini': miniState && !animating }"
      :style="{ width: `${miniState ? miniWidth : width}px` }"
      @mouseenter="onMouseEnter"
      @mouseleave="onMouseLeave"
      @transitionend="onTransitionEnd"
    >
      <div class="drawer-layout__sticky" :style="{ top: `${top}px`, maxHeight: `calc(100vh - ${top}px)` }">
        <slot name="drawer" />
      </div>
    </aside>

    <div class="drawer-layout__content col q-pa-md">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useQuasar } from 'quasar';

interface Props {
  width?: number;
  miniWidth?: number;
  top?: number;
  /** below this screen width (px) the drawer stays collapsed */
  breakpoint?: number;
}

const props = withDefaults(defineProps<Props>(), {
  width: 200,
  miniWidth: 57,
  top: 50,
  breakpoint: 500,
});

const $q = useQuasar();

const miniState = ref(true);
const animating = ref(false);

const canExpand = computed(() => $q.screen.width >= props.breakpoint);

watch(canExpand, (value) => {
  if (!value) {
    miniState.value = true;
    animating.value = false;
  }
});

function onMouseEnter() {
  if (!canExpand.value) return;
  miniState.value = false;
  animating.value = false;
}

function onMouseLeave() {
  // already collapsed (small screen): no width transition will end
  if (miniState.value) return;
  miniState.value = true;
  animating.value = true;
}

function onTransitionEnd(event: TransitionEvent) {
  if (event.target === event.currentTarget && event.propertyName === 'width') {
    animating.value = false;
  }
}
</script>

<style lang="scss" scoped>
.drawer-layout__aside {
  flex: 0 0 auto;
  border-right: 1px solid $grey-4;
  overflow-x: hidden;
  transition: width 0.15s ease;
}

.drawer-layout__sticky {
  position: sticky;
  overflow-x: hidden;
  overflow-y: auto;
  white-space: nowrap;
}

.drawer-layout__aside--mini {
  :deep(.q-item),
  :deep(.q-item__section) {
    text-align: center;
    justify-content: center;
    padding-left: 0;
    padding-right: 0;
    min-width: 0;
  }

  :deep(.q-item__label),
  :deep(.q-item__section--main),
  :deep(.q-item__section--side ~ .q-item__section--side) {
    display: none;
  }
}

.drawer-layout__content {
  min-width: 0;
}
</style>
