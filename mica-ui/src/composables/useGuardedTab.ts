/** an editor with its unsaved-changes guard, as exposed by the panels */
export interface GuardedEditor {
  confirmLeave: () => Promise<boolean>;
}

/**
 * The tab of a page of editors: `selectTab` switches it, unless one of the mounted editors has
 * unsaved changes the user keeps.
 */
export function useGuardedTab(initial: string, editors: () => (GuardedEditor | undefined)[]) {
  const tab = ref(initial);

  async function selectTab(name: string) {
    if (name === tab.value) return;
    for (const editor of editors()) {
      if (editor && !(await editor.confirmLeave())) return;
    }
    tab.value = name;
  }

  return { tab, selectTab };
}
