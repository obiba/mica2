import { describe, expect, it } from 'vitest';
import type { EntityStateDto } from 'src/models/Mica';
import { useDocumentState } from './useDocumentState';

function state(overrides: Partial<EntityStateDto> = {}): EntityStateDto {
  return {
    revisionsAhead: 0,
    revisionStatus: 'DRAFT',
    permissions: { view: true, edit: true, delete: true, publish: true },
    ...overrides,
  };
}

describe('useDocumentState', () => {
  it('is all false without a state', () => {
    const s = useDocumentState(undefined);
    expect(s.status.value).toBeUndefined();
    expect(s.canView.value).toBe(false);
    expect(s.canEdit.value).toBe(false);
    expect(s.canPublish.value).toBe(false);
    expect(s.canChangeStatus.value).toBe(false);
    expect(s.isPublished.value).toBe(false);
    expect(s.hasUnpublishedChanges.value).toBe(true);
  });

  it('edits drafts only', () => {
    expect(useDocumentState(state()).canEdit.value).toBe(true);
    expect(useDocumentState(state({ revisionStatus: 'UNDER_REVIEW' })).canEdit.value).toBe(false);
    expect(useDocumentState(state({ permissions: { edit: false } })).canEdit.value).toBe(false);
  });

  it('publishes documents under review, unpublishes published ones', () => {
    const underReview = useDocumentState(state({ revisionStatus: 'UNDER_REVIEW' }));
    expect(underReview.canPublish.value).toBe(true);
    expect(underReview.canUnpublish.value).toBe(false);

    const published = useDocumentState(state({ revisionStatus: 'UNDER_REVIEW', publishedTag: 'v1' }));
    expect(published.canPublish.value).toBe(true);
    expect(published.hasUnpublishedChanges.value).toBe(false);
    expect(published.canUnpublish.value).toBe(true);
    expect(published.isPublishedOutOfDate.value).toBe(false);

    const outOfDate = useDocumentState(state({ publishedTag: 'v1', revisionsAhead: 2 }));
    expect(outOfDate.isPublishedOutOfDate.value).toBe(true);
    expect(outOfDate.hasUnpublishedChanges.value).toBe(true);
    expect(useDocumentState(state({ permissions: { publish: false }, revisionStatus: 'UNDER_REVIEW' })).canPublish.value).toBe(false);
  });

  it('deletes deleted documents only', () => {
    expect(useDocumentState(state()).canDelete.value).toBe(false);
    expect(useDocumentState(state({ revisionStatus: 'DELETED' })).canDelete.value).toBe(true);
    expect(useDocumentState(state({ revisionStatus: 'DELETED', permissions: { delete: false } })).canDelete.value).toBe(false);
  });

  it('offers the status transitions of the legacy buttons', () => {
    const draft = useDocumentState(state());
    expect(draft.canGoToDraft.value).toBe(false);
    expect(draft.canGoToUnderReview.value).toBe(true);
    expect(draft.canGoToDeleted.value).toBe(true);

    const publishedDraft = useDocumentState(state({ publishedTag: 'v1' }));
    expect(publishedDraft.canGoToUnderReview.value).toBe(false);
    expect(useDocumentState(state({ publishedTag: 'v1', revisionsAhead: 1 })).canGoToUnderReview.value).toBe(true);

    const deleted = useDocumentState(state({ revisionStatus: 'DELETED' }));
    expect(deleted.canGoToDraft.value).toBe(true);
    expect(deleted.canGoToDeleted.value).toBe(false);

    expect(useDocumentState(state({ permissions: { edit: false } })).canGoToDeleted.value).toBe(false);
  });

  it('manages permissions with the publish permission', () => {
    expect(useDocumentState(state()).canManagePermissions.value).toBe(true);
    expect(useDocumentState(state({ permissions: { publish: false } })).canManagePermissions.value).toBe(false);
  });
});
