import type { MaybeRefOrGetter } from 'vue';
import { convertAsf } from '@obiba/quasar-ui-json-form';
import type { AsfDiagnostic } from '@obiba/quasar-ui-json-form';
import { notifyError } from 'src/utils/notify';
import { useFormsStore } from 'src/stores/forms';

export interface EntityFormOptions {
  /** render every control read-only */
  readonly?: MaybeRefOrGetter<boolean>;
}

/**
 * The form configuration of a document type (`/config/{type}/form`), converted from the
 * angular-schema-form dialect to a JSON Forms `(schema, uischema)` pair for `QJsonForm`.
 * Fetched again when the locale changes (the `t()` tokens are resolved by the server).
 */
export function useEntityForm(formPath: MaybeRefOrGetter<string>, options: EntityFormOptions = {}) {
  const formsStore = useFormsStore();
  const { locale } = useI18n({ useScope: 'global' });

  const loading = ref(false);
  const schema = ref<Record<string, unknown>>();
  const uischema = ref<Record<string, unknown>>();
  const diagnostics = ref<AsfDiagnostic[]>([]);
  const ready = computed(() => schema.value !== undefined && uischema.value !== undefined);

  async function load() {
    loading.value = true;
    try {
      const form = await formsStore.getForm(toValue(formPath), locale.value);
      const result = convertAsf(form.schema, form.definition, {
        readonly: toValue(options.readonly) === true,
        logger: (diagnostic: AsfDiagnostic) => {
          const log = diagnostic.level === 'warn' ? console.warn : console.info;
          log(`[${toValue(formPath)}] ${diagnostic.message}`, diagnostic.key ?? '', diagnostic.element ?? '');
        },
      });
      schema.value = result.schema;
      uischema.value = result.uischema;
      diagnostics.value = result.diagnostics;
    } catch (error) {
      schema.value = undefined;
      uischema.value = undefined;
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  watch([() => toValue(formPath), locale, () => toValue(options.readonly)], load, { immediate: true });

  return { loading, ready, schema, uischema, diagnostics, reload: load };
}
