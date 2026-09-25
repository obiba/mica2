<template>
  <div>
    <q-card v-if="hasEvents" flat bordered class="q-mb-md">
      <q-card-section class="q-pb-none text-subtitle1">{{ t('study.timeline') }}</q-card-section>
      <q-card-section>
        <study-timeline
          :populations="study.populations"
          :selected="population && expanded ? { populationId: population.id ?? '', dceId: expanded } : undefined"
          @select="(event) => emit('select', event.populationId, event.dceId)"
        />
      </q-card-section>
    </q-card>

    <div class="row q-col-gutter-md">
      <div class="col-12 col-md-3">
        <q-btn
          v-if="canEdit"
          color="primary"
          icon="add"
          size="sm"
          :label="t('study.add_population')"
          :to="`${base}/population/new`"
          :disable="busy"
          class="q-mb-sm"
        />
        <q-list bordered separator>
          <q-item
            v-for="item in populations"
            :key="item.id ?? ''"
            clickable
            :active="item.id === population?.id"
            @click="emit('select', item.id ?? '')"
          >
            <q-item-section>
              <q-item-label>{{ localized(item.name, locale) || item.id }}</q-item-label>
              <q-item-label caption>
                {{ item.id }} · {{ t('study.dce_count', item.dataCollectionEvents?.length ?? 0) }}
              </q-item-label>
            </q-item-section>
          </q-item>
          <q-item v-if="populations.length === 0">
            <q-item-section class="text-hint">{{ t('study.no_populations') }}</q-item-section>
          </q-item>
        </q-list>
      </div>

      <div v-if="population" class="col-12 col-md-9">
        <div class="row items-center q-mb-sm">
          <div class="text-h6 col">
            {{ localized(population.name, locale) || population.id }}
            <q-badge color="grey-6" :label="population.id" class="q-ml-sm" />
          </div>
          <div v-if="canEdit" class="text-no-wrap">
            <q-btn
              flat
              dense
              size="sm"
              icon="arrow_upward"
              :title="t('members.move_up')"
              :disable="busy || populationIndex === 0"
              @click="movePopulation(-1)"
            />
            <q-btn
              flat
              dense
              size="sm"
              icon="arrow_downward"
              :title="t('members.move_down')"
              :disable="busy || populationIndex === populations.length - 1"
              @click="movePopulation(1)"
            />
            <q-btn
              flat
              dense
              size="sm"
              icon="edit"
              color="primary"
              :title="t('edit')"
              :disable="busy"
              :to="`${base}/population/${population.id}/edit`"
            />
            <q-btn
              flat
              dense
              size="sm"
              icon="delete"
              color="negative"
              :title="t('delete')"
              :disable="busy"
              @click="toRemove = { population }"
            />
          </div>
        </div>

        <div class="row items-center q-mb-sm">
          <div class="text-subtitle1 col">
            {{ t('study.dces') }}
          </div>
          <q-btn
            v-if="canEdit"
            color="primary"
            icon="add"
            size="sm"
            :label="t('study.add_dce')"
            :to="`${base}/population/${population.id}/dce/new`"
            :disable="busy"
          />
        </div>
        <q-list bordered separator>
          <q-expansion-item
            v-for="(dce, index) in events"
            :key="dce.id ?? index"
            :model-value="expanded === dce.id"
            @update:model-value="(open) => emit('select', population?.id ?? '', open ? dce.id : undefined)"
          >
            <template #header>
              <q-item-section avatar>
                <q-badge color="grey-6" :label="dce.id" />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ localized(dce.name, locale) || dce.id }}</q-item-label>
                <q-item-label caption>{{ dcePeriod(dce, t('study.ongoing')) }}</q-item-label>
              </q-item-section>
              <q-item-section side class="text-no-wrap" @click.stop>
                <div>
                  <template v-if="canEdit">
                    <q-btn
                      flat
                      dense
                      size="sm"
                      icon="arrow_upward"
                      :title="t('members.move_up')"
                      :disable="busy || index === 0"
                      @click="moveEvent(index, -1)"
                    />
                    <q-btn
                      flat
                      dense
                      size="sm"
                      icon="arrow_downward"
                      :title="t('members.move_down')"
                      :disable="busy || index === events.length - 1"
                      @click="moveEvent(index, 1)"
                    />
                    <q-btn
                      flat
                      dense
                      size="sm"
                      icon="edit"
                      color="primary"
                      :title="t('edit')"
                      :disable="busy"
                      :to="`${base}/population/${population.id}/dce/${dce.id}/edit`"
                    />
                    <q-btn
                      flat
                      dense
                      size="sm"
                      icon="content_copy"
                      :title="t('study.clone_dce')"
                      :disable="busy"
                      @click="onClone(dce)"
                    />
                  </template>
                  <q-btn flat dense size="sm" icon="folder" :title="t('files.title')" :to="filesRoute(dce)" />
                  <q-btn
                    v-if="canEdit"
                    flat
                    dense
                    size="sm"
                    icon="delete"
                    color="negative"
                    :title="t('delete')"
                    :disable="busy"
                    @click="toRemove = { population, dce }"
                  />
                </div>
              </q-item-section>
            </template>
            <div class="q-pa-md">
              <entity-json-form
                :model-value="toModel(dce, DCE_FIELDS)"
                form-path="/config/data-collection-event/form"
                readonly
              />
            </div>
          </q-expansion-item>
          <q-item v-if="events.length === 0">
            <q-item-section class="text-hint">{{ t('study.no_dces') }}</q-item-section>
          </q-item>
        </q-list>
        <q-expansion-item dense :label="t('study.definition')" header-class="text-subtitle1 q-px-none" class="q-mt-md">
          <entity-json-form
            :model-value="toModel(population, POPULATION_FIELDS)"
            form-path="/config/population/form"
            readonly
          />
        </q-expansion-item>
      </div>
    </div>

    <confirm-dialog
      :model-value="toRemove !== undefined"
      :title="t(toRemove?.dce ? 'study.delete_dce' : 'study.delete_population')"
      :text="t('study.delete_text', { name: removedName })"
      @update:model-value="toRemove = undefined"
      @confirm="onRemove"
    />
  </div>
</template>

<script setup lang="ts">
import { useQuasar } from 'quasar';
import type { PopulationDto, PopulationDto_DataCollectionEventDto as DceDto, StudyDto } from 'src/models/Mica';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import StudyTimeline from 'src/components/studies/StudyTimeline.vue';
import { DCE_FIELDS, POPULATION_FIELDS, toModel } from 'src/composables/useDocumentModel';
import { localized } from 'src/utils/persons';
import { byWeight, cloneEvent, dcePeriod, moveItem } from 'src/utils/studies';

interface Props {
  study: StudyDto;
  /** the id of the population shown, the first one when undefined */
  populationId?: string | undefined;
  /** the id of the event opened */
  dceId?: string | undefined;
  canEdit: boolean;
  /** a change of the study is being saved */
  busy?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  /** the study with the populations changed, to be saved */
  change: [study: StudyDto, params: Record<string, boolean>];
  /** a population is shown, an event of it opened */
  select: [populationId: string, dceId?: string];
}>();
const { t, locale } = useI18n();
const $q = useQuasar();

const base = computed(() => `/individual-study/${props.study.id}`);
const populations = computed(() => byWeight(props.study.populations));
const population = computed(
  () => populations.value.find((item) => item.id === props.populationId) ?? populations.value[0],
);
const populationIndex = computed(() => populations.value.findIndex((item) => item.id === population.value?.id));
const events = computed(() => byWeight(population.value?.dataCollectionEvents));
const expanded = computed(() => props.dceId);
const hasEvents = computed(() => populations.value.some((item) => (item.dataCollectionEvents ?? []).length > 0));

const toRemove = ref<{ population: PopulationDto; dce?: DceDto }>();
const removedName = computed(() => {
  const removed = toRemove.value?.dce ?? toRemove.value?.population;
  return removed ? localized(removed.name, locale.value) || removed.id : '';
});

function filesRoute(dce: DceDto) {
  const path = `${base.value}/population/${population.value?.id}/data-collection-event/${dce.id}`;
  return { path: `${base.value}/files`, query: { path } };
}

/** the study with the events of the population shown replaced */
function withEvents(dataCollectionEvents: DceDto[]): StudyDto {
  return {
    ...props.study,
    populations: populations.value.map((item) =>
      item.id === population.value?.id ? { ...item, dataCollectionEvents } : item,
    ),
  };
}

function movePopulation(delta: number) {
  emit(
    'change',
    { ...props.study, populations: moveItem(populations.value, populationIndex.value, delta) },
    {
      weightChanged: true,
    },
  );
}

function moveEvent(index: number, delta: number) {
  emit('change', withEvents(moveItem(events.value, index, delta)), { weightChanged: true });
}

function onClone(dce: DceDto) {
  $q.dialog({
    title: t('study.clone_dce'),
    message: t('study.clone_dce_text', { name: localized(dce.name, locale.value) || dce.id }),
    prompt: { model: '1', type: 'number', isValid: (value: string) => Number(value) >= 1 && Number(value) <= 50 },
    cancel: true,
  }).onOk((value: string) => {
    if (population.value) emit('change', withEvents(cloneEvent(population.value, dce, Number(value))), {});
  });
}

function onRemove() {
  const removed = toRemove.value;
  if (!removed) return;
  if (removed.dce) {
    emit('change', withEvents(events.value.filter((item) => item.id !== removed.dce?.id)), {});
  } else {
    const remaining = populations.value.filter((item) => item.id !== removed.population.id);
    emit('change', { ...props.study, populations: remaining }, {});
  }
}
</script>
