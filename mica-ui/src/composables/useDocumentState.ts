import type { MaybeRefOrGetter } from 'vue';
import type { EntityStateDto } from 'src/models/Mica';

export const DRAFT = 'DRAFT';
export const UNDER_REVIEW = 'UNDER_REVIEW';
export const DELETED = 'DELETED';

export type RevisionStatus = typeof DRAFT | typeof UNDER_REVIEW | typeof DELETED;

/**
 * What can be done with a document given its state (revision status, publication, permissions):
 * the rules of the legacy `DocumentPermissionsService` and of the status buttons.
 */
export function useDocumentState(state: MaybeRefOrGetter<EntityStateDto | undefined>) {
  const current = computed(() => toValue(state));
  const permissions = computed(() => current.value?.permissions ?? {});

  const status = computed<RevisionStatus | undefined>(
    () => current.value?.revisionStatus as RevisionStatus | undefined,
  );
  const isDraft = computed(() => status.value === DRAFT);
  const isUnderReview = computed(() => status.value === UNDER_REVIEW);
  const isDeleted = computed(() => status.value === DELETED);

  const isPublished = computed(() => current.value?.publishedTag !== undefined);
  const revisionsAhead = computed(() => current.value?.revisionsAhead ?? 0);
  /** published, but the draft has moved on since */
  const isPublishedOutOfDate = computed(() => isPublished.value && revisionsAhead.value > 0);
  /** there is something new to publish */
  const hasUnpublishedChanges = computed(() => !isPublished.value || revisionsAhead.value > 0);

  const canView = computed(() => permissions.value.view === true);
  const canEdit = computed(() => permissions.value.edit === true && isDraft.value);
  const canDelete = computed(() => permissions.value.delete === true && isDeleted.value);
  const canPublish = computed(() => permissions.value.publish === true && isUnderReview.value);
  const canUnpublish = computed(() => permissions.value.publish === true && isPublished.value);
  /** the permissions section (draft permissions and published accesses) */
  const canManagePermissions = computed(() => permissions.value.publish === true);

  const canChangeStatus = computed(() => permissions.value.edit === true);
  const canGoToDraft = computed(() => canChangeStatus.value && !isDraft.value);
  const canGoToUnderReview = computed(() => canChangeStatus.value && isDraft.value && hasUnpublishedChanges.value);
  const canGoToDeleted = computed(() => canChangeStatus.value && !isDeleted.value);

  return {
    status,
    isDraft,
    isUnderReview,
    isDeleted,
    isPublished,
    isPublishedOutOfDate,
    hasUnpublishedChanges,
    revisionsAhead,
    canView,
    canEdit,
    canDelete,
    canPublish,
    canUnpublish,
    canManagePermissions,
    canChangeStatus,
    canGoToDraft,
    canGoToUnderReview,
    canGoToDeleted,
  };
}

/** the filters of a documents list, the counts of the statistics summary: they overlap */
export const STATE_FILTERS = ['PUBLISHED', 'NOT_PUBLISHED', 'IN_EDITION', 'UNDER_REVIEW', 'TO_DELETE'] as const;

export type StateFilter = (typeof STATE_FILTERS)[number];

export function isStateFilter(value: unknown): value is StateFilter {
  return typeof value === 'string' && (STATE_FILTERS as readonly string[]).includes(value);
}

/** whether a document state matches a list filter, `IN_EDITION` being published with a draft ahead */
export function matchesStateFilter(state: EntityStateDto | undefined, filter: StateFilter | undefined): boolean {
  if (!filter) return true;
  const published = state?.publishedTag !== undefined;
  switch (filter) {
    case 'PUBLISHED':
      return published;
    case 'NOT_PUBLISHED':
      return !published;
    case 'IN_EDITION':
      return published && (state?.revisionsAhead ?? 0) > 0;
    case 'UNDER_REVIEW':
      return state?.revisionStatus === UNDER_REVIEW;
    case 'TO_DELETE':
      return state?.revisionStatus === DELETED;
  }
}
