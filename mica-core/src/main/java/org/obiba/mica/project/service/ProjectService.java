package org.obiba.mica.project.service;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestUpdatedEvent;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

@Service
public class ProjectService extends AbstractGitPersistableService<ProjectState, Project> {

  @Inject
  private ProjectRepository projectRepository;

  @Inject
  private ProjectStateRepository projectStateRepository;


  public List<Project> findAllProjects() {
    return projectRepository.findAll();
  }

  public void save(@NotNull Project project) {
    save(project, null);
  }

  public void delete(@NotNull String id) {
    projectRepository.delete(id);
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

  @Override
  public Project findDraft(@NotNull String id) throws NoSuchEntityException {
    Project project = projectRepository.findOne(id);
    if(project == null) throw NoSuchEntityException.withId(Project.class, id);
    return project;
  }

  @Override
  public void save(Project project, String comments) {
    projectRepository.save(project);
    gitService.save(project);
  }

  @Override
  protected String generateId(@NotNull Project gitPersistable) {
    return null;
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
