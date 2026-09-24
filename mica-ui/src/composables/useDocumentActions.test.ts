import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('src/boot/api', () => ({
  api: { get: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));
vi.mock('src/utils/notify', () => ({
  notifyError: vi.fn(),
  notifySuccess: vi.fn(),
}));
vi.mock('src/boot/i18n', () => ({
  t: (key: string, args?: Record<string, string>) => (args ? `${key} ${JSON.stringify(args)}` : key),
}));

import { api } from 'src/boot/api';
import { notifyError, notifySuccess } from 'src/utils/notify';
import { documentTarget } from './useDocumentTarget';
import { conflictError, useDocumentActions } from './useDocumentActions';

const target = documentTarget('network', 'net1');
const mocked = api as unknown as {
  get: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

beforeEach(() => {
  vi.clearAllMocks();
  mocked.get.mockResolvedValue({ data: [] });
  mocked.put.mockResolvedValue({});
  mocked.delete.mockResolvedValue({});
});

describe('useDocumentActions', () => {
  it('publishes without cascading when no file is under review', async () => {
    const { publish } = useDocumentActions(target);
    expect(await publish()).toBe('updated');
    expect(mocked.get).toHaveBeenCalledWith('/draft/files-search/network/net1', {
      params: { recursively: true, query: 'revisionStatus:UNDER_REVIEW' },
    });
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/_publish', null, { params: { cascading: 'NONE' } });
    expect(notifySuccess).toHaveBeenCalledWith('document.published');
  });

  it('publishes the files under review along with the document', async () => {
    mocked.get.mockResolvedValue({ data: [{ path: '/network/net1/a.pdf' }] });
    const { publish } = useDocumentActions(target);
    await publish();
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/_publish', null, {
      params: { cascading: 'UNDER_REVIEW' },
    });
  });

  it('applies the actions to the right endpoints', async () => {
    const { apply } = useDocumentActions(target);
    expect(await apply({ type: 'unpublish' })).toBe('updated');
    expect(mocked.delete).toHaveBeenCalledWith('/draft/network/net1/_publish');
    expect(await apply({ type: 'status', status: 'UNDER_REVIEW' })).toBe('updated');
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/_status', null, { params: { value: 'UNDER_REVIEW' } });
    expect(await apply({ type: 'delete' })).toBe('deleted');
    expect(mocked.delete).toHaveBeenCalledWith('/draft/network/net1');
  });

  it('notifies failures and tracks the busy state', async () => {
    mocked.put.mockRejectedValue(new Error('boom'));
    const { busy, toStatus } = useDocumentActions(target);
    const pending = toStatus('DRAFT');
    expect(busy.value).toBe(true);
    expect(await pending).toBe('failed');
    expect(busy.value).toBe(false);
    expect(notifyError).toHaveBeenCalledWith(expect.objectContaining({ message: 'boom' }));
  });

  it('explains a delete conflict', async () => {
    mocked.delete.mockRejectedValue({ response: { status: 409, data: { network: ['n1', 'n2'], dataset: ['d1'] } } });
    const { remove } = useDocumentActions(target);
    expect(await remove()).toBe('failed');
    expect(notifyError).toHaveBeenCalledWith(
      expect.objectContaining({
        message:
          'document.delete_conflict {"references":"document.references.network: n1, n2; document.references.dataset: d1"}',
      }),
    );
  });
});

describe('conflictError', () => {
  it('passes other errors through', () => {
    const error = { response: { status: 500, data: 'oops' } };
    expect(conflictError(error)).toBe(error);
    const empty = { response: { status: 409, data: {} } };
    expect(conflictError(empty)).toBe(empty);
  });

  it('explains a study save conflict', () => {
    const error = { response: { status: 409, data: { studyDataset: ['ds1'], harmonizationDataset: [] } } };
    expect((conflictError(error, 'study.population_conflict') as Error).message).toBe(
      'study.population_conflict {"references":"document.references.studyDataset: ds1"}',
    );
  });
});
