import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';

export const LOG_LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'] as const;

export type LogLevel = (typeof LOG_LEVELS)[number];

export interface LoggerDto {
  name: string;
  /** the effective level: the one set on the logger, or inherited from its parent */
  level: string;
}

/** the color of a level, as the legacy page: the finer the level, the louder the color */
export const LOG_LEVEL_COLORS: Record<LogLevel, string> = {
  TRACE: 'negative',
  DEBUG: 'warning',
  INFO: 'info',
  WARN: 'positive',
  ERROR: 'primary',
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

  async function load(): Promise<LoggerDto[]> {
    loading.value = true;
    try {
      const response = await api.get<LoggerDto[]>('/logs');
      loggers.value = response.data;
    } catch (error) {
      notifyError(error);
      loggers.value = [];
    } finally {
      loading.value = false;
    }
    return loggers.value;
  }

  /** sets the level of a logger, then reloads the list: the children inherit the new level */
  async function setLevel(name: string, level: LogLevel): Promise<boolean> {
    busy.value = name;
    try {
      await api.put('/logs', { name, level });
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      busy.value = undefined;
    }
  }

  return { loggers, loading, busy, countsByLevel, load, setLevel };
}
