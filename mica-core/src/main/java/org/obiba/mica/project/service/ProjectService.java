package org.obiba.mica.project.service;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestUpdatedEvent;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.event.IndexProjectsEvent;
import org.obiba.mica.project.event.ProjectDeletedEvent;
import org.obiba.mica.project.event.ProjectPublishedEvent;
import org.obiba.mica.project.event.ProjectUnpublishedEvent;
import org.obiba.mica.project.event.ProjectUpdatedEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

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
  private FileSystemService fileSystemService;

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
   * Find a {@link Network} by its ID.
   *
   * @param id
   * @return
   * @throws NoSuchNetworkException
   */
  @NotNull
  public Project findById(@NotNull String id) throws NoSuchNetworkException {
    Project project= projectRepository.findOne(id);

    if(project == null) throw NoSuchMicaProjectException.withId(id);

    return project;
  }

  @Override
  public Project findDraft(@NotNull String id) throws NoSuchEntityException {
    Project project = projectRepository.findOne(id);
    if(project == null) throw NoSuchEntityException.withId(Project.class, id);
    return project;
  }

  @Override
  public void save(Project project, String comments) {
    Project saved = project;

    if(project.isNew()) {
      generateId(saved);
    } else {
      saved = projectRepository.findOne(project.getId());

      if(saved != null) {
        BeanUtils.copyProperties(project, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
            "lastModifiedDate");
      } else {
        saved = project;
      }
    }

    ProjectState projectState = findEntityState(project, () -> {
      ProjectState defaultState = new ProjectState();
      defaultState.setName(project.getName());

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
   * Delete a {@link Network}.
   *
   * @param id
   * @throws NoSuchMicaProjectException
   */
  public void delete(@NotNull String id) throws NoSuchMicaProjectException {
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
   * Set the publication flag on a {@link Network}.
   *
   * @param id
   * @throws NoSuchNetworkException
   */
  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'project'") })
  public void publish(@NotNull String id, boolean publish, PublishCascadingScope cascadingScope) throws NoSuchEntityException {
    Project project = projectRepository.findOne(id);
    if (project == null) return;
    if (publish) {
      publishState(id);
      eventBus.post(new ProjectPublishedEvent(project, getCurrentUsername(), cascadingScope));
    } else {
      unPublishState(id);
      eventBus.post(new ProjectUnpublishedEvent(project));
    }
  }

  /**
   * Get all published {@link Project}s.
   *
   * @return
   */
  public List<Project> findAllPublishedProjects() {
    return findPublishedStates().stream() //
        .filter(projectState -> { //
          return gitService.hasGitRepository(projectState) && !Strings.isNullOrEmpty(projectState.getPublishedTag()); //
        }) //
        .map(projectState -> gitService.readFromTag(projectState, projectState.getPublishedTag(), Project.class)) //
        .collect(toList());
  }

  /**
   * Get all {@link Network}s.
   *
   * @return
   */
  public List<Project> findAllProjects() {
    return projectRepository.findAll();
  }

  /**
   * Index all {@link Network}s.
   */
  public void indexAll() {
    eventBus.post(new IndexProjectsEvent());
  }


  /**
   * Index a specific {@link Network} without updating it.
   *
   * @param id
   * @throws NoSuchNetworkException
   */
  public void index(@NotNull String id) throws NoSuchNetworkException {
    ProjectState projectState = getEntityState(id);
    Project project = findById(id);

    eventBus.post(new ProjectUpdatedEvent(project));

    if(projectState.isPublished()) eventBus.post(new ProjectPublishedEvent(project, getCurrentUsername()));
    else eventBus.post(new ProjectUpdatedEvent(project));
  }

  @Override
  protected String generateId(@NotNull Project project) {
    ensureAcronym(project);
    String nextId = getNextId(project.getAcronym());
    project.setId(nextId);

    return nextId;
  }

  private String getNextId(LocalizedString suggested) {
    if (suggested == null) return null;
    String prefix = suggested.asUrlSafeString().toLowerCase();
    if (Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;
    try {
      findById(next);
      for (int i = 1; i<=1000; i++) {
        next = prefix + "-" + i;
        findById(next);
      }
      return null;
    } catch (NoSuchMicaProjectException e) {
      return next;
    }
  }



  private void ensureAcronym(@NotNull Project project) {
    if (project.getAcronym() == null || project.getAcronym().isEmpty()) {
      project.setAcronym(project.getName().asAcronym());
    }
  }
  //
  // Event handling
  //

  @Async
  @Subscribe
  public void dataAccessRequestUpdated(DataAccessRequestUpdatedEvent event) {
    if (!projectRepository.exists(event.getPersistable().getId())) {
      Project project = new Project();
      project.setId(event.getPersistable().getId());
      save(project, "Created from Data Access Request");
    }
  }

  @Async
  @Subscribe
  public void dataAccessRequestDeleted(DataAccessRequestDeletedEvent event) {
    projectRepository.delete(event.getPersistable().getId());
  }
}
