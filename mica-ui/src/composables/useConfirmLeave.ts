import type { Ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { useQuasar } from 'quasar';

/**
 * The unsaved-changes guard of an editor: `confirmLeave` resolves to true when the editor can be left
 * (nothing to save, or the user gives up the changes), and the route is guarded the same way.
 */
export function useConfirmLeave(dirty: Ref<boolean>) {
  const $q = useQuasar();
  const { t } = useI18n({ useScope: 'global' });

  function confirmLeave(): Promise<boolean> {
    if (!dirty.value) return Promise.resolve(true);
    return new Promise<boolean>((resolve) => {
      $q.dialog({
        title: t('document.unsaved_title'),
        message: t('document.unsaved_text'),
        cancel: true,
        persistent: true,
      })
        .onOk(() => resolve(true))
        .onCancel(() => resolve(false));
    });
  }

  onBeforeRouteLeave(confirmLeave);

  return { confirmLeave };
}
