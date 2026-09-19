import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('src/boot/api', () => ({ api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
vi.mock('src/utils/notify', () => ({ notifyError: vi.fn() }));

import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';
import type { CommentDto } from 'src/models/Mica';
import { canAct, useComments } from './useComments';

const mocked = api as unknown as Record<'get' | 'post' | 'put' | 'delete', ReturnType<typeof vi.fn>>;
const text = { headers: { 'Content-Type': 'text/plain' } };
const comment: CommentDto = {
  id: 'c1',
  message: 'hi',
  resourceId: '/draft/network',
  instanceId: 'net1',
  createdBy: 'bob',
  timestamps: { created: '2026-09-19T07:55:47.797' },
  actions: ['EDIT'],
};

beforeEach(() => {
  vi.clearAllMocks();
  mocked.get.mockResolvedValue({ data: [comment] });
  mocked.post.mockResolvedValue({});
  mocked.put.mockResolvedValue({});
  mocked.delete.mockResolvedValue({});
});

describe('useComments', () => {
  it('loads the thread of the resource', async () => {
    const thread = useComments('/draft/network/net1');
    expect(await thread.load()).toHaveLength(1);
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/comments');
    expect(thread.comments.value[0]?.id).toBe('c1');
    expect(thread.loading.value).toBe(false);
  });

  it('adds a comment as plain text and reloads', async () => {
    const thread = useComments('/draft/network/net1');
    expect(await thread.add('**hello**')).toBe(true);
    expect(mocked.post).toHaveBeenCalledWith('/draft/network/net1/comments', '**hello**', text);
    expect(mocked.get).toHaveBeenCalledWith('/draft/network/net1/comments');
  });

  it('updates and deletes by comment id', async () => {
    const thread = useComments(() => '/draft/network/net1');
    await thread.update(comment, 'edited');
    expect(mocked.put).toHaveBeenCalledWith('/draft/network/net1/comment/c1', 'edited', text);
    await thread.remove(comment);
    expect(mocked.delete).toHaveBeenCalledWith('/draft/network/net1/comment/c1');
    expect(mocked.get).toHaveBeenCalledTimes(2);
  });

  it('reports failures and keeps going', async () => {
    const thread = useComments('/draft/network/net1');
    mocked.post.mockRejectedValueOnce(new Error('nope'));
    expect(await thread.add('x')).toBe(false);
    expect(notifyError).toHaveBeenCalled();
    mocked.get.mockRejectedValueOnce(new Error('nope'));
    expect(await thread.load()).toEqual([]);
  });

  it('tells the granted actions', () => {
    expect(canAct(comment, 'EDIT')).toBe(true);
    expect(canAct(comment, 'DELETE')).toBe(false);
    expect(canAct({ ...comment, actions: undefined as unknown as string[] }, 'EDIT')).toBe(false);
  });
});
