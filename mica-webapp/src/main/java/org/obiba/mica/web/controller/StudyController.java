package org.obiba.mica.web.controller;

import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    BaseStudy study = getStudy(id);
    params.put("study", study);
    params.put("type", (study instanceof Study) ? "Individual" : "Harmonization");
    return new ModelAndView("study", params);
  }

  private BaseStudy getStudy(String id) {
    BaseStudy study = publishedStudyService.findById(id);
    if (study == null) throw NoSuchStudyException.withId(id);
    checkAccess((study instanceof Study) ? "/individual-study" : "/harmonization-study", id);
    return study;
  }

}
