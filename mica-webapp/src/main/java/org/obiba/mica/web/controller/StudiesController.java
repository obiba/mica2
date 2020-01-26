package org.obiba.mica.web.controller;

import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class StudiesController extends EntityController {

  @Inject
  private PublishedStudyService publishedStudyService;

  @GetMapping("/studies")
  public ModelAndView list() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("studies", getStudies());

    return new ModelAndView("studies", params);
  }

  @GetMapping("/individual-studies")
  public ModelAndView listIndividualStudies() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("studies", getStudies().stream().filter(s -> s instanceof Study).collect(Collectors.toList()));
    params.put("type", "Individual");
    return new ModelAndView("studies", params);
  }

  @GetMapping("/harmonization-studies")
  public ModelAndView listHarmonizationStudies() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("studies", getStudies().stream().filter(s -> s instanceof HarmonizationStudy).collect(Collectors.toList()));
    params.put("type", "Harmonization");
    return new ModelAndView("studies", params);
  }

  private List<BaseStudy> getStudies() {
    return publishedStudyService.findAll().stream()
      .filter(s -> subjectAclService.isAccessible((s instanceof Study) ? "/individual-study" : "harmonization-study", s.getId()))
      .collect(Collectors.toList());
  }

}
