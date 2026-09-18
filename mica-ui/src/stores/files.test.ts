import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import type { FileDto } from 'src/models/Mica';
import { FileType } from 'src/models/Mica';
import { useFilesStore } from './files';

function file(name: string, overrides: Partial<FileDto> = {}): FileDto {
  return {
    name,
    path: `/f/${name}`,
    type: FileType.FILE,
    children: [],
    description: [],
    revisionStatus: 'DRAFT',
    permissions: { view: true, edit: true, delete: true, publish: true },
    ...overrides,
  };
}

beforeEach(() => setActivePinia(createPinia()));

describe('files store', () => {
  it('keeps the viewable files on copy, the editable drafts on cut', () => {
    const clipboard = useFilesStore();
    const hidden = file('h', { permissions: { view: false, edit: false, delete: false, publish: false } });
    const reviewed = file('r', { revisionStatus: 'UNDER_REVIEW' });
    clipboard.copy('/f', [file('a'), hidden, reviewed]);
    expect(clipboard.command).toBe('copy');
    expect(clipboard.origin).toBe('/f');
    expect(clipboard.items.map((i) => i.name)).toEqual(['a', 'r']);
    clipboard.cut('/f', [file('a'), hidden, reviewed]);
    expect(clipboard.command).toBe('move');
    expect(clipboard.items.map((i) => i.name)).toEqual(['a']);
    clipboard.cut('/f', [reviewed]);
    expect(clipboard.hasItems).toBe(false);
    expect(clipboard.command).toBeUndefined();
    clipboard.copy('/f', [file('a')]);
    clipboard.clear();
    expect(clipboard.hasItems).toBe(false);
    expect(clipboard.origin).toBeUndefined();
  });
});
