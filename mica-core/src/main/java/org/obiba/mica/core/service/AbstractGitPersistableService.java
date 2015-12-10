package org.obiba.mica.core.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.util.Pair;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.obiba.git.CommitInfo;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.notification.EntityPublicationFlowMailNotification;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import static org.obiba.mica.core.domain.RevisionStatus.DELETED;
import static org.obiba.mica.core.domain.RevisionStatus.DRAFT;

public abstract class AbstractGitPersistableService<T extends EntityState, T1 extends GitPersistable> {

  private static final Logger log = LoggerFactory.getLogger(AbstractGitPersistableService.class);

  @Inject
  protected ObjectMapper objectMapper;

  @Inject
  protected EntityPublicationFlowMailNotification entityPublicationFlowNotification;

  @Inject
  protected GitService gitService;

  protected abstract EntityStateRepository<T> getEntityStateRepository();

  protected abstract Class<T1> getType();

  /**
   * Type name for REST resources and permissions.
   *
   * @return
   */
  public abstract String getTypeName();

  @NotNull
  public abstract T1 findDraft(@NotNull String id) throws NoSuchEntityException;

  public T1 getFromCommit(T1 gitPersistable, String commitId) {
    String blob = gitService.getBlob(gitPersistable, commitId, getType());
    InputStream inputStream = new ByteArrayInputStream(blob.getBytes(StandardCharsets.UTF_8));

    try {
      return objectMapper.readValue(inputStream, getType());
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public abstract void save(T1 gitPersistable, String comments);

  @Nullable
  public T findStateById(@NotNull String id) throws NoSuchEntityException {
    return getEntityStateRepository().findOne(id);
  }

  @NotNull
  public T getEntityState(String id) {
    T entityState = getEntityStateRepository().findOne(id);
    if(entityState == null) throw NoSuchEntityException.withId(getType(), id);
    return entityState;
  }

  @NotNull
  public T findEntityState(T1 gitPersistable, Supplier<T> stateSupplier) {
    T defaultState;

    if(gitPersistable.isNew()) {
      defaultState = stateSupplier.get();
      defaultState.setId(generateId(gitPersistable));
      getEntityStateRepository().save(defaultState);
      gitPersistable.setId(defaultState.getId());

      return defaultState;
    }

    T existingState = getEntityStateRepository().findOne(gitPersistable.getId());

    if(existingState == null) {
      defaultState = stateSupplier.get();
      defaultState.setId(gitPersistable.getId());
      getEntityStateRepository().save(defaultState);

      return defaultState;
    }

    return existingState;
  }

  protected abstract String generateId(@NotNull T1 gitPersistable);

  protected void ensureGitRepository(@NotNull T gitPersistable) {
    if(!gitService.hasGitRepository(gitPersistable)) {
      // not sure what to do here
    }
  }

  public Iterable<CommitInfo> getCommitInfos(@NotNull T1 persistable) {
    return gitService.getCommitsInfo(persistable, persistable.getClass());
  }

  public CommitInfo getCommitInfo(@NotNull T1 persistable, @NotNull String commitInfo) {
    return gitService.getCommitInfo(persistable, commitInfo, persistable.getClass());
  }

  public Iterable<String> getDiffEntries(@NotNull T1 persistable, @NotNull String commitId,
    @Nullable String prevCommitId) {
    return gitService.getDiffEntries(persistable, commitId, prevCommitId, persistable.getClass());
  }

  public List<T> findPublishedStates() {
    return getEntityStateRepository().findByPublishedTagNotNull();
  }

  public List<T> findAllStates() {
    return getEntityStateRepository().findAll();
  }

  public T publishState(@NotNull String id) throws NoSuchEntityException {
    T entityState = findStateById(id);
    if(entityState != null) {
      entityState.setRevisionStatus(DRAFT);
      Pair<String, String> tagInfo = gitService.tag(entityState);
      entityState.setPublishedTag(tagInfo.getFirst());
      entityState.setPublishedId(tagInfo.getSecond());
      entityState.setPublicationDate(DateTime.now());
      entityState.setPublishedBy(getCurrentUsername());
      entityState.resetRevisionsAhead();
      entityState.setPublicationDate(DateTime.now());
      getEntityStateRepository().save(entityState);
    }
    return entityState;
  }

  public T unPublishState(@NotNull String id) {
    T entityState = findStateById(id);
    if(entityState != null) {
      entityState.resetRevisionsAhead();
      entityState.setPublishedTag(null);
      entityState.setPublicationDate(null);
      entityState.setPublishedBy(null);
      if(entityState.getRevisionStatus() != DELETED) entityState.setRevisionStatus(DRAFT);
      getEntityStateRepository().save(entityState);
    }
    return entityState;
  }

  public T updateStatus(String id, RevisionStatus status) {
    T entityState = findStateById(id);
    RevisionStatus current = entityState.getRevisionStatus();
    entityState.setRevisionStatus(status);
    getEntityStateRepository().save(entityState);
    entityPublicationFlowNotification.send(id, getTypeName(), current, status);

    return entityState;
  }

  protected String getCurrentUsername() {
    Subject subject = SecurityUtils.getSubject();
    return subject == null || subject.getPrincipal() == null
      ? AbstractGitWriteCommand.DEFAULT_AUTHOR_NAME
      : subject.getPrincipal().toString();
  }
}
