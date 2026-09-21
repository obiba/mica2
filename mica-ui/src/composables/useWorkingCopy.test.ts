import { describe, expect, it, vi } from 'vitest';
import { defineComponent, nextTick, ref } from 'vue';
import { mount } from '@vue/test-utils';
import { Quasar } from 'quasar';
import { createI18n } from 'vue-i18n';
import messages from 'src/i18n';
import { useWorkingCopy, type WorkingCopyOptions } from './useWorkingCopy';

vi.mock('vue-router', () => ({ onBeforeRouteLeave: vi.fn() }));

interface Config {
  name: string;
  prefix?: string;
}

const i18n = createI18n({ legacy: false, locale: 'en', messages });

/** the composable in a component, as the panels use it */
function setup(options: Partial<WorkingCopyOptions<Config>> = {}) {
  const source = ref<Config | undefined>({ name: 'one' });
  const save = vi.fn(async (value: Config) => {
    source.value = { ...value };
    return true;
  });
  let copy!: ReturnType<typeof useWorkingCopy<Config>>;
  const component = defineComponent({
    setup() {
      copy = useWorkingCopy<Config>({ source: () => source.value, save, ...options });
      return () => null;
    },
  });
  mount(component, { global: { plugins: [Quasar, i18n] } });
  return { source, save, copy };
}

describe('useWorkingCopy', () => {
  it('copies the source, and follows it', async () => {
    const { source, copy } = setup();
    expect(copy.form.value).toEqual({ name: 'one' });
    expect(copy.form.value).not.toBe(source.value);
    expect(copy.dirty.value).toBe(false);

    source.value = { name: 'two' };
    await nextTick();
    expect(copy.form.value).toEqual({ name: 'two' });
  });

  it('is dirty when the copy differs, reset gives the changes up', () => {
    const { copy } = setup();
    copy.form.value!.name = 'edited';
    expect(copy.dirty.value).toBe(true);
    copy.reset();
    expect(copy.form.value).toEqual({ name: 'one' });
    expect(copy.dirty.value).toBe(false);
  });

  it('saves the normalized copy and renews it', async () => {
    const onSaved = vi.fn();
    const { save, copy } = setup({
      normalize: (value) => (value.prefix?.trim() ? value : { name: value.name }),
      onSaved,
    });
    copy.form.value!.prefix = '  ';
    // an empty prefix is none: not a change
    expect(copy.dirty.value).toBe(false);
    copy.form.value!.name = 'edited';
    expect(copy.dirty.value).toBe(true);

    expect(await copy.save()).toBe(true);
    expect(save).toHaveBeenCalledWith({ name: 'edited' });
    expect(onSaved).toHaveBeenCalledTimes(1);
    await nextTick();
    expect(copy.form.value).toEqual({ name: 'edited' });
    expect(copy.dirty.value).toBe(false);
  });

  it('keeps the copy when the save fails', async () => {
    const onSaved = vi.fn();
    const { copy } = setup({ save: async () => false, onSaved });
    copy.form.value!.name = 'edited';
    expect(await copy.save()).toBe(false);
    expect(onSaved).not.toHaveBeenCalled();
    expect(copy.form.value).toEqual({ name: 'edited' });
    expect(copy.dirty.value).toBe(true);
  });

  it('can be left when nothing changed', async () => {
    const { copy } = setup();
    expect(await copy.confirmLeave()).toBe(true);
  });
});
