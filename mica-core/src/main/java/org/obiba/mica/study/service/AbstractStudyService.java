/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.DocumentDifferenceService;
import org.obiba.mica.core.service.StudyIdGeneratorService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.scheduling.annotation.Async;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import static java.util.stream.Collectors.toList;

public abstract class AbstractStudyService<S extends EntityState, T extends BaseStudy>
    extends AbstractGitPersistableService<S, T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractStudyService.class);

  @Inject
  protected EventBus eventBus;

  @Inject
  protected ModelAwareTranslator modelAwareTranslator;

  @Inject
  protected FileSystemService fileSystemService;

  @Inject
  protected StudyIdGeneratorService studyIdGeneratorService;

  @Override
  @NotNull
  @Cacheable(value = "studies-draft", key = "#id")
  public T findDraft(String id) throws NoSuchEntityException {
    return findDraft(id, null);
  }

  public T findDraft(@NotNull String id, String locale) throws NoSuchEntityException {
    // ensure study exists
    getEntityState(id);

    T study = getRepository().findOne(id);

    if (locale != null) {
      modelAwareTranslator.translateModel(locale, study);
    }

    return study;
  }

  public ByteArrayOutputStream writeCsv(String id) throws JsonProcessingException {
    BaseStudy draft = findDraft(id);

    Map<String, Object> map = DocumentDifferenceService.flatten(draft);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (CSVWriter writer = new CSVWriter(new PrintWriter(baos))) {
      writer.writeNext(new String[] { "Field", "Value" });

      map.forEach((key, value) -> {
        writer.writeNext(new String[] { key, value.toString() });
      });

    } catch (IOException e) {
      //
    }

    return baos;
  }

  @NotNull
  public abstract List<String> findAllIds();

  @NotNull
  public List<String> findIds(List<String> ids) {
    return StreamSupport.stream(getRepository().findAll(ids).spliterator(), false)
      .map(AbstractGitPersistable::getId)
      .collect(Collectors.toList());
  }

  @NotNull
  public T findStudy(@NotNull String id) throws NoSuchEntityException {
    // ensure study exists
    T study = getRepository().findOne(id);
    if (study == null)
      throw NoSuchEntityException.withId(getType(), id);
    return study;
  }

  public boolean isPublished(@NotNull String id) throws NoSuchEntityException {
    return getEntityState(id).isPublished();
  }

  public List<T> findAllPublishedStudies() {
    return findAllPublishedStudiesInternal(findPublishedStates());
  }

  public List<T> findAllPublishedStudies(List<String> ids) {
    return findAllPublishedStudiesInternal(findPublishedStates(ids));
  }

  protected List<T> findAllPublishedStudiesInternal(List<S> states) {
    return states.stream() //
      .filter(studyState ->
        gitService.hasGitRepository(studyState) && !Strings.isNullOrEmpty(studyState.getPublishedTag()))
      .map(studyState -> gitService.readFromTag(studyState, studyState.getPublishedTag(), getType()))
      .map(s -> { s.getModel(); return s; }) // make sure dynamic model is initialized
      .collect(toList());
  }

  public List<T> findAllDraftStudies() {
    return getRepository().findAll();
  }

  public List<T> findAllDraftStudies(Iterable<String> ids) {
    return Lists.newArrayList(getRepository().findAll(ids));
  }

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", allEntries = true),
      @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id") })
  public void delete(@NotNull String id) {
    T study = getRepository().findOne(id);

    if (study == null) {
      throw NoSuchEntityException.withId(getType(), id);
    }

    checkStudyConstraints(study);

    fileSystemService.delete(FileUtils.getEntityPath(study));
    getEntityStateRepository().delete(id);
    getRepository().delete(study);
    gitService.deleteGitRepository(study);
    eventBus.post(new StudyDeletedEvent(study));
  }

  protected abstract MongoRepository<T, String> getRepository();

  protected abstract EntityStateRepository<S> getEntityStateRepository();

  protected abstract Class<T> getType();

  @Override
  public String getTypeName() {
    return null;
  }

  /**
   * Index all {@link Study}s.
   */
  public void indexAll() {
    eventBus.post(new IndexStudiesEvent());
  }

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid T study) {
    saveInternal(study, null, true);
  }

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid T study, @Nullable String comment) {
    saveInternal(study, comment, true);
  }

  protected abstract void saveInternal(T study, String comment, boolean cascade);

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", allEntries = true),
      @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id") })
  public void publish(@NotNull String id, boolean publish) throws NoSuchEntityException {
    publish(id, publish, PublishCascadingScope.NONE);
  }

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", allEntries = true),
      @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id") })
  public void publish(@NotNull String id, boolean publish, PublishCascadingScope cascadingScope)
      throws NoSuchEntityException {
    log.info("Publish study: {}", id);
    T study = getRepository().findOne(id);

    if (publish) {
      publishState(id);
      eventBus.post(new StudyPublishedEvent(study, getCurrentUsername(), cascadingScope));
    } else {
      unPublishState(id);
      eventBus.post(new StudyUnpublishedEvent(study));
    }
  }

  protected void ensureAcronym(@NotNull T study) {
    if (study.getAcronym() == null || study.getAcronym().isEmpty()) {
      study.setAcronym(study.getName().asAcronym());
    }
  }

  @Nullable
  @Override
  protected String generateId(@NotNull T study) {
    ensureAcronym(study);
    return studyIdGeneratorService.generateId(study.getAcronym());
  }

  protected abstract void checkStudyConstraints(T study);

  public abstract Map<String, List<String>> getPotentialConflicts(T study, boolean publishing);

  protected void removeRoles(@NotNull T study, Iterable<String> roles) {
    saveInternal(study, String.format("Removed roles: %s", Joiner.on(", ").join(roles)), false);
    S state = findStateById(study.getId());

    if (state.isPublished()) {
      publishState(study.getId());
      eventBus.post(new StudyPublishedEvent(study, getCurrentUsername(), PublishCascadingScope.NONE));
    }
  }
}
