import type { MaybeRefOrGetter } from 'vue';
import { api } from 'src/boot/api';
import type { CommentDto } from 'src/models/Mica';
import { notifyError } from 'src/utils/notify';

/** the comment messages are posted as plain text (markdown) */
const TEXT = { headers: { 'Content-Type': 'text/plain' } };

/**
 * The comments thread of a resource: `{path}/comments` lists and adds, `{path}/comment/{id}`
 * updates and deletes. The `actions` of each comment say what the user may do with it.
 */
export function useComments(path: MaybeRefOrGetter<string>) {
  const comments = ref<CommentDto[]>([]);
  const loading = ref(false);

  async function load(): Promise<CommentDto[]> {
    loading.value = true;
    try {
      const response = await api.get<CommentDto[]>(`${toValue(path)}/comments`);
      comments.value = response.data;
    } catch (error) {
      notifyError(error);
      comments.value = [];
    } finally {
      loading.value = false;
    }
    return comments.value;
  }

  async function add(message: string): Promise<boolean> {
    try {
      await api.post(`${toValue(path)}/comments`, message, TEXT);
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  async function update(comment: CommentDto, message: string): Promise<boolean> {
    try {
      await api.put(`${toValue(path)}/comment/${comment.id}`, message, TEXT);
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  async function remove(comment: CommentDto): Promise<boolean> {
    try {
      await api.delete(`${toValue(path)}/comment/${comment.id}`);
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  return { comments, loading, load, add, update, remove };
}

/** the user may perform this action on the comment, as granted by the server */
export function canAct(comment: CommentDto, action: 'EDIT' | 'DELETE'): boolean {
  return comment.actions?.includes(action) === true;
}
