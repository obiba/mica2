package org.obiba.mica.web.controller;

import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class StudiesController extends EntityController {

  @Inject
  private PublishedStudyService publishedStudyService;

  @GetMapping("/studies")
  public ModelAndView list() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("studies", publishedStudyService.findAll().stream()
      .filter(s -> subjectAclService.isAccessible((s instanceof Study) ? "/individual-study" : "harmonization-study", s.getId()))
      .collect(Collectors.toList()));

    return new ModelAndView("studies", params);
  }

}
