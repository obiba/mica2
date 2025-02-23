package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.DraftProjectService;
import org.obiba.mica.project.service.NoSuchProjectException;
import org.obiba.mica.project.service.PublishedProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProjectsController extends BaseController {

  @Inject
  private PublishedProjectService publishedProjectService;

  @Inject
  private DraftProjectService draftProjectService;

  @GetMapping("/projects")
  public ModelAndView projects() {
    ModelAndView mv = new ModelAndView("projects");
    mv.getModel().put("projects", getProjects());
    return mv;
  }

  @GetMapping("/project/{id:.+}")
  public ModelAndView project(@PathVariable String id, @RequestParam(value = "draft", required = false) String shareKey) {
    ModelAndView mv = new ModelAndView("project");
    mv.getModel().put("project", getProject(id, shareKey));
    mv.getModel().put("draft", !Strings.isNullOrEmpty(shareKey));
    return mv;
  }

  //
  // Private methods
  //

  private Project getProject(String id, String shareKey) {
    Project project;
    if (Strings.isNullOrEmpty(shareKey)) {
      project = publishedProjectService.findById(id);
      if (project == null) throw NoSuchProjectException.withId(id);
      checkAccess("/project", id);
    } else {
      project = draftProjectService.findById(id);
      checkPermission("/draft/project", "VIEW", id, shareKey);
    }
    return project;
  }

  private List<Project> getProjects() {
    try {
      return publishedProjectService.findAll().stream()
        .filter(p -> isAccessible("/project", p.getId()))
        .collect(Collectors.toList());
    } catch (Exception e) {
      return Lists.newArrayList();
    }
  }

}
