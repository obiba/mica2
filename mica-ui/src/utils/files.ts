import type { FileDto } from 'src/models/Mica';

export const FILE_DRAFT = 'DRAFT';
export const FILE_UNDER_REVIEW = 'UNDER_REVIEW';
export const FILE_DELETED = 'DELETED';
export type FileStatus = typeof FILE_DRAFT | typeof FILE_UNDER_REVIEW | typeof FILE_DELETED;

export function isFile(document: FileDto | undefined): boolean {
  return document?.type === 'FILE';
}

export function isFolder(document: FileDto | undefined): boolean {
  return document?.type === 'FOLDER';
}

/** the parent path, `/` for the top-level ones */
export function parentPath(path: string): string {
  const index = path.lastIndexOf('/');
  return index <= 0 ? '/' : path.substring(0, index);
}

export function joinPath(folder: string, name: string): string {
  return folder === '/' ? `/${name}` : `${folder}/${name}`;
}

/** whether `path` is `root` or under it */
export function isUnder(path: string, root: string): boolean {
  return root === '/' || path === root || path.startsWith(`${root}/`);
}

/** a path as a URL segment sequence: each folder name encoded, the separators kept */
export function encodePath(path: string): string {
  return path.split('/').map(encodeURIComponent).join('/');
}

export interface Breadcrumb {
  name: string;
  path: string;
}

/** the crumbs from the root (included, named after its last segment, `/` at the top) to the path */
export function breadcrumbsOf(path: string, root: string): Breadcrumb[] {
  const crumbs: Breadcrumb[] = [{ name: root === '/' ? '/' : root.substring(root.lastIndexOf('/') + 1), path: root }];
  if (!isUnder(path, root) || path === root) return crumbs;
  const relative = root === '/' ? path.substring(1) : path.substring(root.length + 1);
  let current = root;
  relative.split('/').forEach((name) => {
    current = joinPath(current, name);
    crumbs.push({ name, path: current });
  });
  return crumbs;
}

/** a Material icon for the document, by type and extension */
export function fileIcon(document: FileDto | undefined): string {
  if (!document) return '';
  if (document.type === 'FOLDER') return 'folder';
  const extension = (document.name.match(/\.(\w+)$/)?.[1] ?? '').toLowerCase();
  switch (extension) {
    case 'doc':
    case 'docx':
    case 'odt':
    case 'gdoc':
    case 'txt':
    case 'md':
      return 'article';
    case 'xls':
    case 'xlsx':
    case 'ods':
    case 'csv':
      return 'table_chart';
    case 'pdf':
      return 'picture_as_pdf';
    case 'ppt':
    case 'pptx':
    case 'odp':
      return 'slideshow';
    case 'png':
    case 'jpg':
    case 'jpeg':
    case 'gif':
    case 'svg':
    case 'webp':
      return 'image';
    case 'zip':
    case 'gz':
    case 'tgz':
    case '7z':
      return 'folder_zip';
    default:
      return 'description';
  }
}

export function sizeLabel(size: number | undefined): string {
  if (size === undefined || size === null || isNaN(size)) return '';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = size;
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index++;
  }
  return `${index === 0 ? value : value.toFixed(1)} ${units[index]}`;
}

export function isPublished(document: FileDto): boolean {
  return document.state?.publicationDate !== undefined;
}

/** the draft is the published revision: nothing to publish or review */
export function isUpToDate(document: FileDto): boolean {
  return document.state?.attachment?.id !== undefined && document.state.attachment.id === document.state.publishedId;
}

export function canView(document: FileDto): boolean {
  return document.permissions?.view === true;
}

export function canEdit(document: FileDto): boolean {
  return document.permissions?.edit === true && document.revisionStatus === FILE_DRAFT;
}

export function canDelete(document: FileDto): boolean {
  return document.permissions?.delete === true && document.revisionStatus === FILE_DELETED;
}

export function canPublish(document: FileDto): boolean {
  return document.permissions?.publish === true && document.revisionStatus === FILE_UNDER_REVIEW;
}

export function canUnpublish(document: FileDto): boolean {
  return document.permissions?.publish === true && isPublished(document);
}

export function canGoTo(document: FileDto, status: FileStatus): boolean {
  switch (status) {
    case FILE_DRAFT:
      return document.revisionStatus !== FILE_DRAFT;
    case FILE_UNDER_REVIEW:
      return document.revisionStatus === FILE_DRAFT && !isUpToDate(document);
    case FILE_DELETED:
      return document.revisionStatus !== FILE_DELETED;
  }
}
