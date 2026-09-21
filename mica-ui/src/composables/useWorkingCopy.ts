import type { Ref } from 'vue';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';

export interface WorkingCopyOptions<T extends object> {
  /** the saved value, from the state: the copy is renewed whenever it changes */
  source: () => T | undefined;
  /** saves the value, true when saved: the source is expected to be reloaded */
  save: (value: T) => Promise<boolean>;
  /** the value as saved (an empty optional field removed): what is compared, what is sent */
  normalize?: (value: T) => T;
  /** called when the copy has been saved */
  onSaved?: () => void;
}

/**
 * A working copy of a saved value, edited in place and discarded on cancel: `dirty` when it differs
 * from the source, `reset` to give the changes up, `save` to send it, `confirmLeave` as the
 * unsaved-changes guard of the editor.
 */
export function useWorkingCopy<T extends object>(options: WorkingCopyOptions<T>) {
  const normalize = options.normalize ?? ((value: T) => value);
  const form = ref<T>() as Ref<T | undefined>;

  const dirty = computed(() => {
    const source = options.source();
    return (
      form.value !== undefined &&
      source !== undefined &&
      JSON.stringify(normalize(form.value)) !== JSON.stringify(normalize(source))
    );
  });
  const { confirmLeave } = useConfirmLeave(dirty);

  /** the copy as the source is */
  function reset() {
    const source = options.source();
    form.value = source ? structuredClone(toRaw(source)) : undefined;
  }

  async function save(): Promise<boolean> {
    if (!form.value) return false;
    const saved = await options.save(normalize(form.value));
    if (saved) {
      options.onSaved?.();
      reset();
    }
    return saved;
  }

  watch(options.source, reset, { immediate: true });

  return { form, dirty, reset, save, confirmLeave };
}
