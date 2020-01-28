package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ProjectsController {

  @GetMapping("/projects")
  public ModelAndView get() {
    return new ModelAndView("projects");
  }

}
