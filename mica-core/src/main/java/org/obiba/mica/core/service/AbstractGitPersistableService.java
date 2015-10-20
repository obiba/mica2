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

import org.joda.time.DateTime;
import org.obiba.git.CommitInfo;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.study.domain.Study;
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
  protected GitService gitService;

  @Inject
  protected EntityStateRepository<T> entityStateRepository;

  protected abstract GitPersistable unpublish(T gitPersistable);

  protected abstract Class<T> getType();

  @NotNull
  public abstract T1 findDraft(@NotNull String id) throws NoSuchEntityException;

  public T1 getFromCommit(T1 gitPersistable, String commitId) {
    String blob = gitService.getBlob(gitPersistable, commitId, getType());
    InputStream inputStream = new ByteArrayInputStream(blob.getBytes(StandardCharsets.UTF_8));

    try {
      return (T1) objectMapper.readValue(inputStream, getType());
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public abstract void save(T1 gitPersistable, String comments);

  @NotNull
  public T findStateById(@NotNull String id) throws NoSuchEntityException {
    T entityState = getEntityState(id);

    ensureGitRepositoryAndSave(entityState);

    return entityState;
  }

  protected T getEntityState(String id) {
    T entityState = entityStateRepository.findOne(id);

    if(entityState == null) throw NoSuchEntityException.withId(getType(), id);

    return entityState;
  }

  @NotNull
  protected T findEntityState(T1 gitPersistable, Supplier<T> stateSupplier) {
    T defaultState;

    if(gitPersistable.isNew()) {
      defaultState = stateSupplier.get();
      defaultState.setId(generateId(gitPersistable));
      entityStateRepository.save(defaultState);
      gitPersistable.setId(defaultState.getId());

      return defaultState;
    }

    T existingState = entityStateRepository.findOne(gitPersistable.getId());

    if(existingState == null) {
      defaultState = stateSupplier.get();
      defaultState.setId(gitPersistable.getId());
      entityStateRepository.save(defaultState);

      return defaultState;
    }

    return existingState;
  }

  protected abstract String generateId(@NotNull T1 gitPersistable);

  protected void ensureGitRepository(@NotNull T gitPersistable) {
    if(!gitService.hasGitRepository(gitPersistable)) {
      unpublish(gitPersistable);
    }
  }

  protected void ensureGitRepositoryAndSave(@NotNull T gitPersistable) {
    if(!gitService.hasGitRepository(gitPersistable)) {
      GitPersistable recovered = unpublish(gitPersistable);

      if(recovered != null) {
        log.info("Recuperated state '{}' from repository is saved backed to Git repo.", gitPersistable.getId());
        gitService.save(recovered);
      }
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
    return entityStateRepository.findByPublishedTagNotNull();
  }

  public List<T> findAllStates() {
    return entityStateRepository.findAll();
  }

  public T publishState(@NotNull String id) throws NoSuchEntityException {
    T entityState = findStateById(id);
    entityState.setRevisionStatus(DRAFT);
    entityState.setPublishedTag(gitService.tag(entityState));
    entityState.resetRevisionsAhead();
    entityState.setPublicationDate(DateTime.now());
    entityStateRepository.save(entityState);

    return entityState;
  }

  protected void unpublishState(@NotNull T entityState) {
    entityState.resetRevisionsAhead();
    entityState.setPublishedTag(null);
    entityState.setPublicationDate(null);

    if(entityState.getRevisionStatus() != DELETED) entityState.setRevisionStatus(DRAFT);

    entityStateRepository.save(entityState);
  }

  @Nullable
  public GitPersistable unPublish(String id) {
    T entityState = entityStateRepository.findOne(id);

    return entityState == null ? null : unpublish(entityState);
  }

  public T updateStatus(String id, RevisionStatus status) {
    T entityState = findStateById(id);
    entityState.setRevisionStatus(status);
    entityStateRepository.save(entityState);

    return entityState;
  }
}
