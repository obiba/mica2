package org.obiba.mica.project.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.project.service.PublishedProjectService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/projects/index/health")
@Scope("request")
public class ProjectsIndexHealthResource extends EntityIndexHealthResource<Project> {

  final private ProjectService projectService;

  final private ProjectStateRepository projectStateRepository;

  final private PublishedProjectService publishedProjectService;

  @Inject
  public ProjectsIndexHealthResource(ProjectService projectService,
                                     ProjectStateRepository projectStateRepository,
                                     PublishedProjectService publishedProjectService) {
    this.projectService = projectService;
    this.projectStateRepository = projectStateRepository;
    this.publishedProjectService = publishedProjectService;
  }

  @Override
  protected List<Project> findAllPublished() {
    List<String> ids = projectStateRepository.findByPublishedTagNotNull()
      .stream()
      .map(ProjectState::getId)
      .collect(Collectors.toList());
    return projectService.findAllPublishedProjects(ids);
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedProjectService.suggest(MAX_VALUE, "en", createEsQuery(Project.class), ES_QUERY_FIELDS, null);
  }

  @Override
  protected LocalizedString getEntityTitle(Project entity) {
    return entity.getTitle();
  }

  @Override
  protected String createEsQuery(Class clazz) {
    return String.format("*");
  }
}
