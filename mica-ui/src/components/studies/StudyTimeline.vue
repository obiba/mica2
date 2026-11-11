<template>
  <div v-if="timeline" :class="['study-timeline', { 'no-labels': !labels }]">
    <div class="row justify-end">
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
    <div v-for="(population, index) in timeline.populations" :key="population.id" class="row no-wrap q-mb-xs">
      <div class="timeline-label text-caption ellipsis q-pr-sm" :title="population.label">
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
            :class="[
              'timeline-bar',
              `bg-${color(index)}`,
              {
                ongoing: event.ongoing,
                selected: selected?.populationId === event.populationId && selected?.dceId === event.dceId,
              },
            ]"
            :style="{ left: `${position(event.start)}%`, width: `${position(event.end) - position(event.start)}%` }"
            @click="emit('select', event)"
          >
            <span v-if="labels" class="ellipsis">{{ event.label || event.dceId }}</span>
            <q-tooltip>
              <div class="text-weight-bold">{{ event.label || event.dceId }}</div>
              <div>{{ population.label }}</div>
              <div>{{ period(event) }}</div>
            </q-tooltip>
          </div>
        </div>
      </div>
    </div>
    <div class="row no-wrap">
      <div class="timeline-label" />
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
  </div>
</template>

<script setup lang="ts">
import type { PopulationDto } from 'src/models/Mica';
import { studyTimeline, type TimelineEvent } from 'src/utils/studies';

interface Props {
  populations: PopulationDto[];
  /** the event to highlight */
  selected?: { populationId: string; dceId: string } | undefined;
  /** the color index of the first population, to keep its color of the study timeline */
  colorOffset?: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{ select: [event: TimelineEvent] }>();
const { t, locale } = useI18n();

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
const years = computed(() => {
  if (!timeline.value) return [];
  const first = timeline.value.min.getFullYear();
  return Array.from({ length: timeline.value.max.getFullYear() - first }, (_, index) => first + index);
});
const step = computed(() => Math.max(1, Math.ceil(years.value.length / MAX_LABELS)));

function color(index: number) {
  return COLORS[(index + (props.colorOffset ?? 0)) % COLORS.length];
}

/** the position of a date on the track, in percent */
function position(date: Date) {
  const { min, max } = timeline.value ?? { min: date, max: date };
  return ((date.getTime() - min.getTime()) / (max.getTime() - min.getTime() || 1)) * 100;
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
}
/* without the names, thinner bars */
.no-labels .timeline-lane,
.no-labels .timeline-bar {
  height: 12px;
}
.no-labels .timeline-label {
  line-height: 12px;
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
