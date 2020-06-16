/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestUpdatedEvent;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.event.*;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class ProjectService extends AbstractGitPersistableService<ProjectState, Project> {

  @Inject
  private ProjectRepository projectRepository;

  @Inject
  private ProjectStateRepository projectStateRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private MicaConfigService micaConfigService;

  public void save(@NotNull Project project) {
    save(project, null);
  }

  @Override
  protected EntityStateRepository<ProjectState> getEntityStateRepository() {
    return projectStateRepository;
  }

  @Override
  protected Class<Project> getType() {
    return Project.class;
  }

  @Override
  public String getTypeName() {
    return "project";
  }

  /**
   * Find a {@link Project} by its ID.
   *
   * @param id
   * @return
   * @throws NoSuchProjectException
   */
  @NotNull
  public Project findById(@NotNull String id) throws NoSuchProjectException {
    Project project = projectRepository.findOne(id);

    if(project == null) throw NoSuchProjectException.withId(id);

    return project;
  }

  @Override
  public Project findDraft(@NotNull String id) throws NoSuchEntityException {
    Project project = projectRepository.findOne(id);
    if(project == null) throw NoSuchEntityException.withId(Project.class, id);
    return project;
  }

  @Override
  public void save(@NotNull @Valid Project project, String comments) {
    Project saved = project;

    if(project.isNew()) {
      generateId(saved);
    } else {
      saved = projectRepository.findOne(project.getId());

      if(saved != null) {
        BeanUtils.copyProperties(project, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "dataAccessRequestId");
      } else {
        saved = project;
      }
    }

    ProjectState projectState = findEntityState(project, () -> {
      ProjectState defaultState = new ProjectState();
      defaultState.setTitle(project.getTitle());

      return defaultState;
    });

    if(!project.isNew()) ensureGitRepository(projectState);

    projectState.incrementRevisionsAhead();
    projectStateRepository.save(projectState);

    saved.setLastModifiedDate(DateTime.now());

    projectRepository.save(saved);
    eventBus.post(new ProjectUpdatedEvent(saved));
    gitService.save(saved);
  }

  /**
   * Delete a {@link Project}.
   *
   * @param id
   * @throws NoSuchProjectException
   */
  public void delete(@NotNull String id) throws NoSuchProjectException {
    Project project = findById(id);
    fileSystemService.delete(FileUtils.getEntityPath(project));
    projectStateRepository.delete(id);
    projectRepository.delete(id);
    gitService.deleteGitRepository(project);
    eventBus.post(new ProjectDeletedEvent(project));
  }

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'project'") })
  public void publish(@NotNull String id, boolean publish) throws NoSuchEntityException {
    publish(id, publish, PublishCascadingScope.NONE);
  }

  /**
   * Set the publication flag on a {@link Project}.
   *
   * @param id
   * @throws NoSuchProjectException
   */
  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'project'") })
  public void publish(@NotNull String id, boolean publish, PublishCascadingScope cascadingScope)
    throws NoSuchEntityException {
    Project project = projectRepository.findOne(id);
    if(project == null) return;
    if(publish) {
      publishState(id);
      eventBus.post(new ProjectPublishedEvent(project, getCurrentUsername(), cascadingScope));
    } else {
      unPublishState(id);
      eventBus.post(new ProjectUnpublishedEvent(project));
    }
  }

  /**
   * Get all project identifiers.
   *
   * @return
   */
  public List<String> findAllIds() {
    return projectRepository.findAllExistingIds().stream().map(Project::getId).collect(toList());
  }

  /**
   * Get all published {@link Project}s.
   *
   * @return
   */
  public List<Project> findAllPublishedProjects() {
    return findAllPublishedProjectsInternal(findPublishedStates());
  }

  public List<Project> findAllPublishedProjects(List<String> ids) {
    return findAllPublishedProjectsInternal(findPublishedStates(ids));
  }

  private List<Project> findAllPublishedProjectsInternal(List<ProjectState> states) {
    return states.stream() //
      .filter(projectState -> { //
        return gitService.hasGitRepository(projectState) && !Strings.isNullOrEmpty(projectState.getPublishedTag()); //
      }) //
      .map(projectState -> gitService.readFromTag(projectState, projectState.getPublishedTag(), Project.class)) //
      .collect(toList());
  }

  /**
   * Get all {@link Project}s.
   *
   * @return
   */
  public List<Project> findAllProjects() {
    return projectRepository.findAll();
  }

  /**
   * Get all {@link Project}s by ids.
   *
   * @return
   */
  public List<Project> findAllProjects(Iterable<String> ids) {
    return Lists.newArrayList(projectRepository.findAll(ids));
  }

  /**
   * Index all {@link Project}s.
   */
  public void indexAll() {
    eventBus.post(new IndexProjectsEvent());
  }

  /**
   * Index a specific {@link Project} without updating it.
   *
   * @param id
   * @throws NoSuchProjectException
   */
  public void index(@NotNull String id) throws NoSuchProjectException {
    ProjectState projectState = getEntityState(id);
    Project project = findById(id);

    eventBus.post(new ProjectUpdatedEvent(project));

    if(projectState.isPublished()) eventBus.post(new ProjectPublishedEvent(project, getCurrentUsername()));
    else eventBus.post(new ProjectUpdatedEvent(project));
  }

  @Override
  protected String generateId(@NotNull Project project) {
    if (!project.hasTitle()) return null;
    String nextId = getNextId(project.getTitle().asAcronym());
    project.setId(nextId);
    return nextId;
  }

  private String getNextId(LocalizedString suggested) {
    if(suggested == null) return null;
    String prefix = suggested.asUrlSafeString().toLowerCase();
    if(Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;

    if(!projectRepository.exists(next)) return next;
    for(int i = 1; i <= 1000; i++) {
      next = prefix + "-" + i;
      if(!projectRepository.exists(next)) return next;
    }
    return null;
  }


  //
  // Event handling
  //

  @Async
  @Subscribe
  public void dataAccessRequestUpdated(DataAccessRequestUpdatedEvent event) {
    DataAccessRequest request = event.getPersistable();
    if(!projectRepository.exists(request.getId()) && request.getStatus() == DataAccessEntityStatus.APPROVED) {
      Project project = new Project();
      project.setId(event.getPersistable().getId());
      project.setDataAccessRequestId(event.getPersistable().getId());
      String title = dataAccessRequestUtilService.getRequestTitle(request);
      if(!Strings.isNullOrEmpty(title)) {
        project.setTitle(LocalizedString.from(micaConfigService.getConfig().getLocales(), title));
      }
      String summary = dataAccessRequestUtilService.getRequestSummary(request);
      if(!Strings.isNullOrEmpty(summary)) {
        project.setSummary(LocalizedString.from(micaConfigService.getConfig().getLocales(), summary));
      }
      save(project, "Created from Data Access Request");
    }
  }

  @Async
  @Subscribe
  public void dataAccessRequestDeleted(DataAccessRequestDeletedEvent event) {
    delete(event.getPersistable().getId());
  }
}
