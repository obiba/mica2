import { describe, expect, it, vi } from 'vitest';
import { useGuardedTab } from './useGuardedTab';

describe('useGuardedTab', () => {
  it('switches the tab when the editors can be left', async () => {
    const editor = { confirmLeave: vi.fn().mockResolvedValue(true) };
    const { tab, selectTab } = useGuardedTab('a', () => [editor, undefined]);
    await selectTab('b');
    expect(tab.value).toBe('b');
    expect(editor.confirmLeave).toHaveBeenCalledTimes(1);
  });

  it('keeps the tab when an editor has changes the user keeps', async () => {
    const kept = { confirmLeave: vi.fn().mockResolvedValue(false) };
    const other = { confirmLeave: vi.fn().mockResolvedValue(true) };
    const { tab, selectTab } = useGuardedTab('a', () => [kept, other]);
    await selectTab('b');
    expect(tab.value).toBe('a');
    // the other editors are not asked
    expect(other.confirmLeave).not.toHaveBeenCalled();
  });

  it('does not ask the editors for the current tab', async () => {
    const editor = { confirmLeave: vi.fn().mockResolvedValue(false) };
    const { tab, selectTab } = useGuardedTab('a', () => [editor]);
    await selectTab('a');
    expect(tab.value).toBe('a');
    expect(editor.confirmLeave).not.toHaveBeenCalled();
  });
});
