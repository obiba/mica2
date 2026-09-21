import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';

/** the logback levels, from the finest to the coarsest */
export const LOG_LEVELS = ['ALL', 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'] as const;

export type LogLevel = (typeof LOG_LEVELS)[number];

export interface LoggerDto {
  name: string;
  /** the effective level: the one set on the logger, or inherited from its parent */
  level: string;
}

/** the color of a level, as the legacy page: the finer the level, the louder the color */
export const LOG_LEVEL_COLORS: Record<LogLevel, string> = {
  ALL: 'purple',
  TRACE: 'negative',
  DEBUG: 'warning',
  INFO: 'info',
  WARN: 'positive',
  ERROR: 'primary',
  OFF: 'grey-7',
};

/**
 * The loggers of the server (`/logs`) with their effective level, which can be changed on the fly.
 */
export function useLoggers() {
  const loggers = ref<LoggerDto[]>([]);
  const loading = ref(false);
  /** the name of the logger whose level is being changed */
  const busy = ref<string>();

  /** the number of loggers at each level */
  const countsByLevel = computed(() => {
    const counts = Object.fromEntries(LOG_LEVELS.map((level) => [level, 0])) as Record<LogLevel, number>;
    loggers.value.forEach((logger) => {
      if (logger.level in counts) counts[logger.level as LogLevel]++;
    });
    return counts;
  });

  /** loads the loggers, and tells whether they could be loaded */
  async function load(): Promise<boolean> {
    loading.value = true;
    try {
      const response = await api.get<LoggerDto[]>('/logs');
      loggers.value = response.data;
      return true;
    } catch (error) {
      notifyError(error);
      loggers.value = [];
      return false;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Sets the level of a logger, then reloads the list: the children inherit the new level. Succeeds only when
   * the reloaded list shows the change.
   */
  async function setLevel(name: string, level: LogLevel): Promise<boolean> {
    busy.value = name;
    try {
      await api.put('/logs', { name, level });
      return await load();
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      busy.value = undefined;
    }
  }

  return { loggers, loading, busy, countsByLevel, load, setLevel };
}
