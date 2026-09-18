import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { Quasar } from 'quasar';
import { createPinia, setActivePinia } from 'pinia';
import { i18n } from 'src/boot/i18n';
import networkForm from '../../../test/fixtures/network-form.json';
import EntityJsonForm from './EntityJsonForm.vue';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn() },
  toServerUrl: (path: string) => path,
}));

import { api } from 'src/boot/api';
import { useSystemStore } from 'src/stores/system';
import { useFormsStore } from 'src/stores/forms';

function mountForm(props: { modelValue: Record<string, unknown>; readonly?: boolean }) {
  return mount(EntityJsonForm, {
    props: { formPath: '/config/network/form', ...props },
    global: { plugins: [Quasar, i18n] },
  });
}

beforeEach(() => {
  setActivePinia(createPinia());
  useSystemStore().configuration.languages = ['en', 'fr'];
  useFormsStore().clear();
  vi.mocked(api.get).mockResolvedValue({ data: networkForm });
});

describe('EntityJsonForm', () => {
  it('renders the network form served by Mica (mandatory part + custom part)', async () => {
    const wrapper = mountForm({
      modelValue: { website: 'https://example.org', _name: { en: 'Net' }, _acronym: { en: 'N' }, _description: { en: 'd' } },
    });
    await flushPromises();
    expect(api.get).toHaveBeenCalledWith('/config/network/form', { params: { locale: 'en' } });
    const text = wrapper.text();
    expect(text).toContain('General Information');
    expect(text).toContain('Name');
    expect(text).toContain('Acronym');
    expect(text).toContain('Description');
    expect(text).toContain('Website');
    const website = wrapper.findAll('input').find((i) => (i.element as HTMLInputElement).value === 'https://example.org');
    expect(website).toBeDefined();
  });

  it('reports the missing required fields once validated', async () => {
    const wrapper = mountForm({ modelValue: { website: '' } });
    await flushPromises();
    // hidden until validate() is called
    expect(wrapper.emitted('update:errors')).toBeDefined();
    const form = wrapper.vm as unknown as { validate: () => boolean };
    expect(form.validate()).toBe(false);
    await flushPromises();
    expect(wrapper.text()).toContain('This field is required');
  });

  it('accepts a complete document', async () => {
    const wrapper = mountForm({
      modelValue: { _name: { en: 'Net', fr: 'Rés' }, _acronym: { en: 'N', fr: 'N' }, _description: { en: 'd', fr: 'd' } },
    });
    await flushPromises();
    const form = wrapper.vm as unknown as { validate: () => boolean };
    expect(form.validate()).toBe(true);
  });

  it('renders read-only without inputs to edit', async () => {
    const wrapper = mountForm({ modelValue: { website: 'x', _name: { en: 'Net' } }, readonly: true });
    await flushPromises();
    const editable = wrapper.findAll('input, textarea').filter((i) => !(i.element as HTMLInputElement).disabled && !(i.element as HTMLInputElement).readOnly);
    expect(editable.length).toBe(0);
  });
});
