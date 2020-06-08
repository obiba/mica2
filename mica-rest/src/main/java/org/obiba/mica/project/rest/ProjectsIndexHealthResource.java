package org.obiba.mica.project.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.project.service.PublishedProjectService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;

@Component
@Path("/projects/index/health")
@Scope("request")
public class ProjectsIndexHealthResource extends EntityIndexHealthResource<Project> {

  final private ProjectService projectService;

  final private PublishedProjectService publishedProjectService;

  @Inject
  public ProjectsIndexHealthResource(ProjectService projectService,
                                     PublishedProjectService publishedProjectService) {
    this.projectService = projectService;
    this.publishedProjectService = publishedProjectService;
  }


  @Override
  protected List<Project> findAllPublished() {
    return projectService.findAllPublishedProjects();
  }


  @Override
  protected List<String> findAllIndexedIds() {
    return publishedProjectService.suggest(MAX_VALUE, "en", createEsQuery(Project.class), ES_QUERY_FIELDS);
  }

  @Override
  protected String getEntityTitle(Project entity, String locale) {
    return entity.getTitle().get(locale);
  }

  @Override
  protected String createEsQuery(Class clazz) {
    return String.format("*");
  }
}
