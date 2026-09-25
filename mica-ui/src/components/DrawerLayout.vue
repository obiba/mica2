<template>
  <div class="drawer-layout row no-wrap">
    <aside
      class="drawer-layout__aside bg-grey-3 print-hide"
      :class="{ 'drawer-layout__aside--mini': miniState && !animating }"
      :style="{ width: `${miniWidth}px` }"
      @mouseenter="onMouseEnter"
      @mouseleave="onMouseLeave"
    >
      <div
        ref="drawer"
        class="drawer-layout__sticky bg-grey-3"
        :style="{ top: `${top}px`, maxHeight: `calc(100vh - ${top}px)`, width: drawerWidth }"
        @transitionend="onTransitionEnd"
      >
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
  miniWidth?: number;
  top?: number;
  /** below this screen width (px) the drawer stays collapsed */
  breakpoint?: number;
}

const props = withDefaults(defineProps<Props>(), {
  miniWidth: 57,
  top: 50,
  breakpoint: 500,
});

const $q = useQuasar();

const drawer = ref<HTMLElement>();
const miniState = ref(true);
const animating = ref(false);
// measured once expanded, so collapsing back can be animated
const expandedWidth = ref(0);

const drawerWidth = computed(() => {
  if (miniState.value) return `${props.miniWidth}px`;
  return expandedWidth.value ? `${expandedWidth.value}px` : 'max-content';
});

const canExpand = computed(() => $q.screen.width >= props.breakpoint);

watch(canExpand, (value) => {
  if (!value) {
    miniState.value = true;
    animating.value = false;
  }
});

async function onMouseEnter() {
  if (!canExpand.value) return;
  miniState.value = false;
  animating.value = false;
  // re-measuring while the width transition runs would latch the mini width
  if (expandedWidth.value) return;
  await nextTick();
  expandedWidth.value = drawer.value?.offsetWidth ?? 0;
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
}

.drawer-layout__sticky {
  position: sticky;
  z-index: 1;
  border-right: 1px solid $grey-4;
  margin-right: -1px;
  overflow-x: hidden;
  overflow-y: auto;
  white-space: nowrap;
  transition: width 0.15s ease;
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
