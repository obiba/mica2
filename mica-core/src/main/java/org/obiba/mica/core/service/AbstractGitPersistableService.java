/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.commons.math3.util.Pair;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.obiba.git.CommitInfo;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.DefaultEntityBase;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.EntityStateFilter;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.notification.EntityPublicationFlowMailNotification;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.obiba.mica.core.domain.RevisionStatus.DELETED;
import static org.obiba.mica.core.domain.RevisionStatus.DRAFT;
import static org.obiba.mica.core.domain.RevisionStatus.UNDER_REVIEW;

public abstract class AbstractGitPersistableService<T extends EntityState, T1 extends GitPersistable> {

  private static final Logger log = LoggerFactory.getLogger(AbstractGitPersistableService.class);

  private static final String PUBLISHED_CACHE_KEY = "published";

  @Inject
  protected ObjectMapper objectMapper;

  @Inject
  protected EntityPublicationFlowMailNotification entityPublicationFlowNotification;

  @Inject
  protected GitService gitService;

  private Cache<String, List<String>> idsCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();

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

  public T1 getFromCommit(@NotNull T1 gitPersistable, @NotNull String commitId) {
    String blob = gitService.getBlob(gitPersistable, commitId, getType());
    InputStream inputStream = new ByteArrayInputStream(blob.getBytes(StandardCharsets.UTF_8));

    try {
      return objectMapper.readValue(inputStream, getType());
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public abstract void save(@NotNull @Valid T1 gitPersistable, String comments);

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


  public List<String> getIdsByStateFilter(EntityStateFilter filter) {
    EntityStateRepository<T> repository = getEntityStateRepository();
    List<T> entities = new ArrayList<>();

    switch (filter) {
      case PUBLISHED:
        entities = repository.findAllPublishedIds();
        break;
      case UNDER_REVIEW:
        entities = repository.findAllByRevisionStatusIds(UNDER_REVIEW.toString());
        break;
      case TO_DELETE:
        entities = repository.findAllByRevisionStatusIds(DELETED.toString());
        break;
      case IN_EDITION:
        entities = repository.findAllInEditionIds();
        break;
      default:
        entities = repository.findAllExistingIds();
        break;
    }

    return entities.stream().map(DefaultEntityBase::getId).collect(Collectors.toList());
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

  public List<T> findPublishedStates(List<String> ids) {
    return getEntityStateRepository().findByPublishedTagNotNullAndIdIn(ids);
  }

  /**
   * Get the published entity identifiers from the repository (with short-term cache).
   *
   * @return
   */
  public List<String> findPublishedIds() {
    try {
      return idsCache.get(PUBLISHED_CACHE_KEY, this::findPublishedIdsFromRepository);
    } catch (ExecutionException e) {
      return findPublishedIdsFromRepository();
    }
  }

  public List<T> findAllStates() {
    return getEntityStateRepository().findAll();
  }

  public List<T> findAllStates(Iterable<String> ids) {
    return Lists.newArrayList(getEntityStateRepository().findAll(ids));
  }

  public T saveState(@NotNull T entityState) {
    return getEntityStateRepository().save(entityState);
  }

  protected T publishStateInternal(@NotNull String id) {
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
    }

    return entityState;
  }

  public T publishState(@NotNull String id) throws NoSuchEntityException {
    T entityState = publishStateInternal(id);
    if(entityState != null) {
      getEntityStateRepository().save(entityState);
    }
    idsCache.invalidate(PUBLISHED_CACHE_KEY);
    return entityState;
  }

  protected T unPublishStateInternal(@NotNull String id) {
    T entityState = findStateById(id);
    if(entityState != null) {
      entityState.resetRevisionsAhead();
      entityState.setPublishedTag(null);
      entityState.setPublicationDate(null);
      entityState.setPublishedBy(null);
      if(entityState.getRevisionStatus() != DELETED) entityState.setRevisionStatus(DRAFT);
    }

    return entityState;
  }

  public T unPublishState(@NotNull String id) {
    T entityState = unPublishStateInternal(id);
    if(entityState != null) {
      getEntityStateRepository().save(entityState);
    }
    idsCache.invalidate(PUBLISHED_CACHE_KEY);
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
    return extractAuthorName(subject, AbstractGitWriteCommand.DEFAULT_AUTHOR_NAME);
  }

  //
  // Private methods
  //

  private List<String> findPublishedIdsFromRepository() {
    return getEntityStateRepository().findAllPublishedIds().stream().map(DefaultEntityBase::getId).collect(Collectors.toList());
  }

  private String extractAuthorName(Subject subject, String defaultName) {
    try {
      if (subject != null && subject.getPrincipal() != null)
        return subject.getPrincipal().toString();
    } catch (UnknownSessionException ignore) {
    }
    return defaultName;
  }
}
