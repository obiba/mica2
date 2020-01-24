package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
public class StudyController extends EntityController {

  @GetMapping("/study/{id}")
  public ModelAndView study(@PathVariable String id) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("studyId", id);

    return new ModelAndView("study", params);
  }

}
