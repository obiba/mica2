package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.PublishedProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProjectsController extends BaseController {

  @Inject
  private PublishedProjectService publishedProjectService;

  @GetMapping("/projects")
  public ModelAndView get() {
    ModelAndView mv = new ModelAndView("projects");
    mv.getModel().put("projects", getProjects());
    return mv;
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
