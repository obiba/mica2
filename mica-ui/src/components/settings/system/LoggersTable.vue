<template>
  <div>
    <div class="row items-center q-col-gutter-md q-mb-md">
      <div class="col-12 col-md-4">
        <q-input v-model="filter" dense outlined clearable debounce="300" :placeholder="t('config.logs.filter')">
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
        </q-input>
      </div>
      <div class="col-12 col-md-8">
        <div class="row items-center q-gutter-xs">
          <q-chip
            clickable
            :outline="levelFilter !== undefined"
            color="grey-8"
            :text-color="levelFilter === undefined ? 'white' : 'grey-8'"
            @click="levelFilter = undefined"
          >
            {{ t('config.logs.all_levels') }}
            <q-badge
              :color="levelFilter === undefined ? 'white' : 'grey-8'"
              :text-color="levelFilter === undefined ? 'grey-8' : 'white'"
              class="q-ml-sm"
            >
              {{ loggers.length }}
            </q-badge>
          </q-chip>
          <q-chip
            v-for="level in LOG_LEVELS"
            :key="level"
            clickable
            :outline="levelFilter !== level"
            :color="LOG_LEVEL_COLORS[level]"
            :text-color="levelFilter === level ? 'white' : LOG_LEVEL_COLORS[level]"
            @click="levelFilter = levelFilter === level ? undefined : level"
          >
            {{ level }}
            <q-badge
              :color="levelFilter === level ? 'white' : LOG_LEVEL_COLORS[level]"
              :text-color="levelFilter === level ? LOG_LEVEL_COLORS[level] : 'white'"
              class="q-ml-sm"
            >
              {{ countsByLevel[level] }}
            </q-badge>
          </q-chip>
        </div>
      </div>
    </div>
    <q-table
      flat
      dense
      :rows="rows"
      :columns="columns"
      row-key="name"
      :loading="loading"
      :pagination="{ rowsPerPage: 50, sortBy: 'name' }"
      :rows-per-page-options="[25, 50, 100, 0]"
      :no-data-label="t('config.logs.count', 0)"
    >
      <template v-slot:body-cell-name="props">
        <q-td :props="props">
          <span class="logger-name">{{ props.value }}</span>
        </q-td>
      </template>
      <template v-slot:body-cell-level="props">
        <q-td :props="props">
          <q-btn-toggle
            :model-value="props.row.level"
            dense
            unelevated
            no-caps
            size="sm"
            toggle-text-color="white"
            :toggle-color="LOG_LEVEL_COLORS[props.row.level as LogLevel] ?? 'grey-7'"
            color="grey-3"
            text-color="grey-8"
            :options="levelOptions"
            :disable="busy !== undefined"
            @update:model-value="(level: LogLevel) => onSetLevel(props.row.name, level)"
          />
          <q-spinner v-if="busy === props.row.name" color="primary" size="1.2em" class="q-ml-sm" />
        </q-td>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import { LOG_LEVELS, LOG_LEVEL_COLORS, useLoggers, type LoggerDto, type LogLevel } from 'src/composables/useLoggers';
import { notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const { loggers, loading, busy, countsByLevel, load, setLevel } = useLoggers();

const filter = ref<string | null>('');
const levelFilter = ref<LogLevel>();

const levelOptions = LOG_LEVELS.map((level) => ({ label: level, value: level }));

const rows = computed(() => {
  const text = (filter.value ?? '').trim().toLowerCase();
  return loggers.value.filter(
    (logger) =>
      (levelFilter.value === undefined || logger.level === levelFilter.value) &&
      (text === '' || logger.name.toLowerCase().includes(text)),
  );
});

const columns = computed<QTableColumn<LoggerDto>[]>(() => [
  { name: 'name', label: t('name'), field: 'name', sortable: true, align: 'left' },
  { name: 'level', label: t('config.logs.level'), field: 'level', sortable: true, align: 'left' },
]);

async function onSetLevel(name: string, level: LogLevel) {
  if (await setLevel(name, level)) notifySuccess(t('config.logs.level_set', { level }));
}

onMounted(load);
</script>

<style scoped>
.logger-name {
  font-family: monospace;
  font-size: 0.85rem;
}
</style>
