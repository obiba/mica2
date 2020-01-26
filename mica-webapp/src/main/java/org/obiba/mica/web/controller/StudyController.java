package org.obiba.mica.web.controller;

import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Controller
public class StudyController extends EntityController {

  @Inject
  private PublishedStudyService publishedStudyService;

  @GetMapping("/study/{id}")
  public ModelAndView study(@PathVariable String id) {
    Map<String, Object> params = new HashMap<String, Object>();
    BaseStudy study = publishedStudyService.findById(id);
    params.put("study", study);

    return new ModelAndView("study", params);
  }

}
