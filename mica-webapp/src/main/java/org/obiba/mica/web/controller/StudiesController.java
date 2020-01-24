package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
public class StudiesController {

  @GetMapping("/studies")
  public ModelAndView list() {

    return new ModelAndView("studies");
  }

}
