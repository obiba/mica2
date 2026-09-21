import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from 'src/boot/api';
import { useLoggers } from './useLoggers';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;
const list = [
  { name: 'ROOT', level: 'INFO' },
  { name: 'org.obiba.mica', level: 'DEBUG' },
  { name: 'org.obiba.mica.core', level: 'DEBUG' },
];

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useLoggers', () => {
  it('lists the loggers and counts them by level', async () => {
    mocked.get.mockResolvedValueOnce({ data: list });
    const { loggers, countsByLevel, load } = useLoggers();
    await load();
    expect(mocked.get).toHaveBeenCalledWith('/logs');
    expect(loggers.value).toEqual(list);
    expect(countsByLevel.value).toEqual({ TRACE: 0, DEBUG: 2, INFO: 1, WARN: 0, ERROR: 0 });
  });

  it('sets a level then reloads the list', async () => {
    mocked.put.mockResolvedValueOnce({ status: 204 });
    mocked.get.mockResolvedValueOnce({ data: [{ name: 'ROOT', level: 'WARN' }] });
    const { loggers, busy, setLevel } = useLoggers();
    const pending = setLevel('ROOT', 'WARN');
    expect(busy.value).toBe('ROOT');
    expect(await pending).toBe(true);
    expect(mocked.put).toHaveBeenCalledWith('/logs', { name: 'ROOT', level: 'WARN' });
    expect(loggers.value).toEqual([{ name: 'ROOT', level: 'WARN' }]);
    expect(busy.value).toBeUndefined();
  });

  it('reports a failed level change without reloading', async () => {
    mocked.put.mockRejectedValueOnce(new Error('500'));
    const { setLevel } = useLoggers();
    expect(await setLevel('ROOT', 'TRACE')).toBe(false);
    expect(mocked.get).not.toHaveBeenCalled();
  });

  it('empties the list when it cannot be loaded', async () => {
    mocked.get.mockRejectedValueOnce(new Error('403'));
    const { loggers, load } = useLoggers();
    expect(await load()).toEqual([]);
    expect(loggers.value).toEqual([]);
  });
});
