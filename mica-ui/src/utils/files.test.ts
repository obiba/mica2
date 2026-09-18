import { describe, expect, it } from 'vitest';
import type { FileDto } from 'src/models/Mica';
import { FileType } from 'src/models/Mica';
import { breadcrumbsOf, canGoTo, canPublish, fileIcon, isUnder, isUpToDate, joinPath, parentPath, sizeLabel } from './files';

function file(overrides: Partial<FileDto> = {}): FileDto {
  return {
    name: 'a.pdf',
    path: '/network/n/a.pdf',
    type: FileType.FILE,
    children: [],
    description: [],
    revisionStatus: 'DRAFT',
    permissions: { view: true, edit: true, delete: true, publish: true },
    state: { id: 's', name: 'a.pdf', path: '/network/n/a.pdf', revisions: [], attachment: { id: 'v2', fileName: 'a.pdf', description: [], attributes: [] }, publishedId: 'v1' },
    ...overrides,
  };
}

describe('paths', () => {
  it('computes parents and joins', () => {
    expect(parentPath('/network/n/a.pdf')).toBe('/network/n');
    expect(parentPath('/network')).toBe('/');
    expect(parentPath('/')).toBe('/');
    expect(joinPath('/', 'network')).toBe('/network');
    expect(joinPath('/network', 'n')).toBe('/network/n');
    expect(isUnder('/network/n/x', '/network/n')).toBe(true);
    expect(isUnder('/network/nx', '/network/n')).toBe(false);
    expect(isUnder('/anything', '/')).toBe(true);
  });

  it('builds the breadcrumbs from the root', () => {
    expect(breadcrumbsOf('/network/n/docs/a.pdf', '/network/n')).toEqual([
      { name: 'n', path: '/network/n' },
      { name: 'docs', path: '/network/n/docs' },
      { name: 'a.pdf', path: '/network/n/docs/a.pdf' },
    ]);
    expect(breadcrumbsOf('/network', '/')).toEqual([
      { name: '/', path: '/' },
      { name: 'network', path: '/network' },
    ]);
    expect(breadcrumbsOf('/', '/')).toEqual([{ name: '/', path: '/' }]);
    expect(breadcrumbsOf('/elsewhere', '/network/n')).toEqual([{ name: 'n', path: '/network/n' }]);
  });
});

describe('files', () => {
  it('picks icons and formats sizes', () => {
    expect(fileIcon(file({ type: FileType.FOLDER }))).toBe('folder');
    expect(fileIcon(file({ name: 'x.PDF' }))).toBe('picture_as_pdf');
    expect(fileIcon(file({ name: 'x.docx' }))).toBe('article');
    expect(fileIcon(file({ name: 'x' }))).toBe('description');
    expect(sizeLabel(512)).toBe('512 B');
    expect(sizeLabel(2048)).toBe('2.0 KB');
    expect(sizeLabel(undefined)).toBe('');
  });

  it('applies the status rules of the legacy file buttons', () => {
    expect(isUpToDate(file())).toBe(false);
    expect(canGoTo(file(), 'UNDER_REVIEW')).toBe(true);
    expect(canGoTo(file({ state: { id: 's', name: 'a', path: '/p', revisions: [], attachment: { id: 'v1', fileName: 'a', description: [], attributes: [] }, publishedId: 'v1' } }), 'UNDER_REVIEW')).toBe(false);
    expect(canGoTo(file(), 'DRAFT')).toBe(false);
    expect(canGoTo(file({ revisionStatus: 'DELETED' }), 'DELETED')).toBe(false);
    expect(canPublish(file())).toBe(false);
    expect(canPublish(file({ revisionStatus: 'UNDER_REVIEW' }))).toBe(true);
  });
});
