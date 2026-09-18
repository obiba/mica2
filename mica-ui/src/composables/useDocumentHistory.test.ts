import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('src/boot/api', () => ({ api: { get: vi.fn(), put: vi.fn() } }));
vi.mock('src/utils/notify', () => ({ notifyError: vi.fn(), notifySuccess: vi.fn() }));
vi.mock('src/boot/i18n', () => ({ t: (key: string) => key }));
vi.mock('vue-i18n', () => ({ useI18n: () => ({ locale: { value: 'fr' } }) }));

import { api } from 'src/boot/api';
import { documentTarget } from './useDocumentTarget';
import { useDocumentHistory } from './useDocumentHistory';

const target = documentTarget('network', 'net1');
const mocked = api as unknown as { get: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn> };

beforeEach(() => {
  vi.clearAllMocks();
  mocked.put.mockResolvedValue({});
});

describe('useDocumentHistory', () => {
  it('lists the commits and reads a revision and a diff', async () => {
    mocked.get.mockResolvedValueOnce({ data: [{ commitId: 'c1' }, { commitId: 'c2' }] });
    const history = useDocumentHistory(target);
    expect(await history.fetchCommits()).toHaveLength(2);
    expect(history.commits.value[1]?.commitId).toBe('c2');
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/commits');

    mocked.get.mockResolvedValueOnce({ data: { id: 'net1' } });
    expect(await history.viewRevision('c2')).toEqual({ id: 'net1' });
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/commit/c2/view');

    mocked.get.mockResolvedValueOnce({ data: { onlyLeft: {}, differing: {}, onlyRight: {} } });
    await history.diff('c1', 'c2');
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/_diff', { params: { left: 'c1', right: 'c2', locale: 'fr' } });
  });

  it('restores a revision', async () => {
    const history = useDocumentHistory(target);
    expect(await history.restore('c2')).toBe('updated');
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/commit/c2/restore');
  });

  it('restores chosen fields into the current document', async () => {
    mocked.get.mockResolvedValueOnce({
      data: { id: 'net1', name: [{ lang: 'en', value: 'Net' }, { lang: 'fr', value: 'Réseau' }], acronym: [{ lang: 'en', value: 'N' }], content: '{"website":"a"}' },
    });
    const history = useDocumentHistory(target);
    expect(await history.restoreFields(['name.fr', { name: 'model.website', value: 'b' }])).toBe('updated');
    expect(mocked.put).toHaveBeenCalledWith(
      '/draft/network/net1',
      { id: 'net1', name: [{ lang: 'en', value: 'Net' }], acronym: [{ lang: 'en', value: 'N' }], description: undefined, content: '{"website":"b"}' },
      { params: { comment: 'history.restored_fields_comment' } },
    );
  });

  it('reports a failure', async () => {
    mocked.put.mockRejectedValueOnce(new Error('boom'));
    const history = useDocumentHistory(target);
    expect(await history.restore('c9')).toBe('failed');
  });
});
