import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { Quasar } from 'quasar';
import { createPinia, setActivePinia } from 'pinia';
import { i18n } from 'src/boot/i18n';
import type { StudyDto } from 'src/models/Mica';
import EntityJsonForm from 'src/components/forms/EntityJsonForm.vue';
import StudyPopulationsPanel from './StudyPopulationsPanel.vue';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn() },
  toServerUrl: (path: string) => path,
}));

const study = {
  id: 'cls',
  populations: [
    {
      id: 'p1',
      weight: 0,
      dataCollectionEvents: [
        { id: 'd1', weight: 0, startYear: 2000 },
        { id: 'd2', weight: 1, startYear: 2001 },
        { id: 'd3', weight: 2, startYear: 2002 },
      ],
    },
  ],
} as unknown as StudyDto;

function mountPanel(dceId?: string) {
  return mount(StudyPopulationsPanel, {
    props: { study, populationId: 'p1', dceId, canEdit: false },
    global: {
      plugins: [Quasar, i18n],
      stubs: { EntityJsonForm: true, StudyTimeline: true, RouterLink: true },
    },
  });
}

beforeEach(() => {
  setActivePinia(createPinia());
});

describe('StudyPopulationsPanel', () => {
  it('renders the form of the expanded event only', () => {
    const forms = mountPanel('d2').findAllComponents(EntityJsonForm);
    expect(forms.map((form) => form.props('formPath'))).toEqual([
      '/config/population/form',
      '/config/data-collection-event/form',
    ]);
  });

  it('renders no event form when none is expanded', () => {
    const forms = mountPanel().findAllComponents(EntityJsonForm);
    expect(forms.map((form) => form.props('formPath'))).toEqual(['/config/population/form']);
  });
});
