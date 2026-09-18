import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import { Quasar } from 'quasar';
import { createI18n } from 'vue-i18n';
import messages from 'src/i18n';
import type { EntityStateDto } from 'src/models/Mica';
import DocumentStatusButtons from './DocumentStatusButtons.vue';

const i18n = createI18n({ legacy: false, locale: 'en', messages });

function mountButtons(state: EntityStateDto) {
  return mount(DocumentStatusButtons, {
    props: { id: 'net1', state },
    global: { plugins: [Quasar, i18n] },
  });
}

function state(overrides: Partial<EntityStateDto> = {}): EntityStateDto {
  return {
    revisionsAhead: 0,
    revisionStatus: 'DRAFT',
    permissions: { view: true, edit: true, delete: true, publish: true },
    ...overrides,
  };
}

describe('DocumentStatusButtons', () => {
  it('shows the status menu only for a draft that can be edited', () => {
    expect(mountButtons(state()).text()).toContain('Draft');
    expect(mountButtons(state({ permissions: { edit: false } })).text()).toBe('');
  });

  it('offers publish for a document under review', async () => {
    const wrapper = mountButtons(state({ revisionStatus: 'UNDER_REVIEW' }));
    const publish = wrapper.findAll('button').find((b) => b.text().includes('Publish'));
    expect(publish).toBeDefined();
    await publish!.trigger('click');
    expect(wrapper.emitted('action')).toEqual([[{ type: 'publish' }]]);
  });

  it('offers unpublish for a published document and delete for a deleted one', async () => {
    const published = mountButtons(state({ publishedTag: 'v1' }));
    const unpublish = published.findAll('button').find((b) => b.text().includes('Unpublish'));
    await unpublish!.trigger('click');
    expect(published.emitted('action')).toEqual([[{ type: 'unpublish' }]]);

    const deleted = mountButtons(state({ revisionStatus: 'DELETED' }));
    expect(deleted.findAll('button').some((b) => b.text().includes('Remove'))).toBe(true);
    // the delete is confirmed first: nothing emitted on click
    await deleted.findAll('button').find((b) => b.text().includes('Remove'))!.trigger('click');
    expect(deleted.emitted('action')).toBeUndefined();
  });
});
