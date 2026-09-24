import { useFormsStore } from 'src/stores/forms';
import type { Messages } from 'src/utils/formTranslations';

/**
 * The labels of the membership roles, from the Mica translations (`contact.label.{role}`, which can
 * be customized), the role itself when it has none.
 */
export function useRoleLabels() {
  const formsStore = useFormsStore();
  const { locale } = useI18n({ useScope: 'global' });
  const messages = ref<Messages>({});

  watch(
    locale,
    async (language) => {
      messages.value = await formsStore.getBundle(language);
    },
    { immediate: true },
  );

  function roleLabel(role: string): string {
    return messages.value[`contact.label.${role}`] ?? role;
  }

  return { roleLabel };
}
