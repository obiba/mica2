<template>
  <div v-if="rows.length > 0" :class="['study-timeline', { 'no-labels': !labels }]">
    <div class="row justify-end print-hide">
      <q-btn
        flat
        dense
        round
        size="sm"
        :icon="labels ? 'subtitles' : 'subtitles_off'"
        :color="labels ? 'primary' : 'grey-6'"
        :title="t(labels ? 'study.hide_labels' : 'study.show_labels')"
        @click="toggleLabels"
      />
    </div>
    <div v-for="{ population, color } in rows" :key="population.id" class="row no-wrap q-mb-xs">
      <div
        v-if="populationId === undefined"
        class="timeline-label text-caption ellipsis q-pr-sm"
        :title="population.label"
      >
        {{ population.label || population.id }}
      </div>
      <div class="col timeline-track">
        <div
          v-for="year in years"
          :key="year"
          class="timeline-grid"
          :style="{ left: `${position(new Date(year, 0, 1))}%` }"
        />
        <div v-for="(lane, laneIndex) in population.lanes" :key="laneIndex" class="timeline-lane">
          <div
            v-for="event in lane"
            :key="event.dceId"
            :class="['timeline-bar', `bg-${color}`, { ongoing: event.ongoing, selected: isSelected(event) }]"
            :style="{ left: `${position(event.start)}%`, width: `${position(event.end) - position(event.start)}%` }"
            @click="(dom) => onBarClick(dom, event, population.label)"
          >
            <span v-if="labels" class="ellipsis">{{ event.label || event.dceId }}</span>
            <!-- not over the menu of the event -->
            <q-tooltip v-if="!isMenuOf(event)">
              <div class="text-weight-bold">{{ event.label || event.dceId }}</div>
              <div>{{ population.label }}</div>
              <div>{{ period(event) }}</div>
            </q-tooltip>
          </div>
        </div>
      </div>
    </div>
    <div class="row no-wrap">
      <div v-if="populationId === undefined" class="timeline-label" />
      <div class="col timeline-track timeline-axis">
        <div
          v-for="(year, index) in years"
          :key="year"
          class="timeline-tick text-caption text-grey-7"
          :style="{ left: `${position(new Date(year, 0, 1))}%` }"
        >
          {{ index % step === 0 ? year : '' }}
        </div>
      </div>
    </div>
    <!-- one menu for all the bars, the tooltip content with the actions, anchored to the bar clicked;
         the click also selects the event, a route change, which would close the menu at once -->
    <!-- always rendered: created open, it would find no anchor yet and close itself -->
    <q-menu
      v-if="$slots.actions"
      v-model="menuOpen"
      :target="menu?.target ?? false"
      no-parent-event
      no-route-dismiss
      anchor="bottom left"
      self="top left"
      :offset="[0, 4]"
      class="bg-grey-9 text-white text-caption"
    >
      <template v-if="menu">
        <div class="row justify-end no-wrap q-px-xs q-pt-xs">
          <slot name="actions" :event="menu.event" />
          <q-btn flat dense round size="sm" icon="close" :title="t('close')" v-close-popup />
        </div>
        <div class="q-px-sm q-pb-sm">
          <div class="text-weight-bold">{{ menu.event.label || menu.event.dceId }}</div>
          <div>{{ menu.populationLabel }}</div>
          <div>{{ period(menu.event) }}</div>
        </div>
      </template>
    </q-menu>
  </div>
</template>

<script setup lang="ts">
import type { PopulationDto } from 'src/models/Mica';
import { studyTimeline, type TimelineEvent } from 'src/utils/studies';

interface Props {
  populations: PopulationDto[];
  /** the event to highlight */
  selected?: { populationId: string; dceId: string } | undefined;
  /** only the row of this population, without its label, on the scale and in the color of the study timeline */
  populationId?: string | undefined;
}

const props = defineProps<Props>();
const emit = defineEmits<{ select: [event: TimelineEvent] }>();
/** the actions of an event, in a menu opened by a click on its bar (no menu without them) */
const slots = defineSlots<{ actions?: (props: { event: TimelineEvent }) => unknown }>();
const { t, locale } = useI18n();

/** the event of the menu, with the bar it is anchored to */
const menu = ref<{ event: TimelineEvent; populationLabel: string; target: Element }>();
const menuOpen = ref(false);

function isMenuOf(event: TimelineEvent) {
  const shown = menu.value?.event;
  return menuOpen.value && shown?.populationId === event.populationId && shown?.dceId === event.dceId;
}

async function onBarClick(dom: MouseEvent, event: TimelineEvent, populationLabel: string) {
  emit('select', event);
  if (!slots.actions) return;
  const target = dom.currentTarget as Element;
  if (menuOpen.value) {
    menuOpen.value = false;
    // a second click on the bar closes its menu
    if (menu.value?.target === target) return;
    await nextTick();
  }
  menu.value = { event, populationLabel, target };
  // opened once the menu is anchored to the bar
  await nextTick();
  menuOpen.value = true;
}

const COLORS = ['primary', 'teal', 'orange-8', 'purple', 'brown', 'indigo', 'pink-7', 'green-8'];
/** the most year labels on the axis */
const MAX_LABELS = 15;

/** the choice to show the event names in the bars, kept for all the timelines */
const LABELS_KEY = 'mica.study-timeline.labels';
const labels = ref(readLabels());

function readLabels() {
  try {
    return localStorage.getItem(LABELS_KEY) !== 'false';
  } catch {
    return true;
  }
}

function toggleLabels() {
  labels.value = !labels.value;
  try {
    localStorage.setItem(LABELS_KEY, String(labels.value));
  } catch {
    // the choice is not kept, e.g. in a private window
  }
}

const timeline = computed(() => studyTimeline(props.populations, locale.value));
// the study changed (saved, reloaded): the bars are new, the menu would be anchored to a removed one
watch(timeline, () => (menuOpen.value = false));
const years = computed(() => {
  if (!timeline.value) return [];
  const first = timeline.value.min.getFullYear();
  return Array.from({ length: timeline.value.max.getFullYear() - first }, (_, index) => first + index);
});
const step = computed(() => Math.max(1, Math.ceil(years.value.length / MAX_LABELS)));
/** the populations shown with their color, the one of their index on the study timeline */
const rows = computed(() =>
  (timeline.value?.populations ?? [])
    .map((population, index) => ({ population, color: COLORS[index % COLORS.length] }))
    .filter(({ population }) => props.populationId === undefined || population.id === props.populationId),
);

/** the position of a date on the track, in percent */
function position(date: Date) {
  const { min, max } = timeline.value ?? { min: date, max: date };
  return ((date.getTime() - min.getTime()) / (max.getTime() - min.getTime() || 1)) * 100;
}

function isSelected(event: TimelineEvent) {
  return props.selected?.populationId === event.populationId && props.selected?.dceId === event.dceId;
}

function period(event: TimelineEvent) {
  const format = (date: Date) => date.toLocaleDateString(locale.value, { year: 'numeric', month: 'short' });
  return `${format(event.start)} – ${event.ongoing ? t('study.ongoing') : format(event.end)}`;
}
</script>

<style scoped>
/* the bars are background colors, which browsers drop when printing */
.study-timeline {
  print-color-adjust: exact;
  -webkit-print-color-adjust: exact;
}
.timeline-label {
  width: 160px;
  flex: none;
  line-height: 22px;
}
.timeline-track {
  position: relative;
}
.timeline-lane {
  position: relative;
  height: 22px;
  margin-bottom: 2px;
}
.timeline-grid {
  position: absolute;
  top: 0;
  bottom: 0;
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}
.timeline-bar {
  position: absolute;
  top: 0;
  height: 22px;
  min-width: 4px;
  padding: 0 6px;
  border-radius: 4px;
  color: white;
  font-size: 12px;
  line-height: 22px;
  cursor: pointer;
  display: flex;
  overflow: hidden;
}
.timeline-bar.ongoing {
  mask-image: linear-gradient(to right, black 70%, rgba(0, 0, 0, 0.35));
}
.timeline-bar.selected {
  outline: 2px solid black;
  outline-offset: 1px;
}/* without the names, thinner bars */
.no-labels .timeline-lane,
.no-labels .timeline-bar {
  height: 12px;
}
/* not the bar height: the descenders of the population name would be cut */
.no-labels .timeline-label {
  line-height: 16px;
}
.timeline-axis {
  /* the border and the year labels (caption line height), else they overflow the tab panel, which scrolls */
  height: 21px;
  border-top: 1px solid rgba(0, 0, 0, 0.2);
}
.timeline-tick {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  white-space: nowrap;
}
</style>
