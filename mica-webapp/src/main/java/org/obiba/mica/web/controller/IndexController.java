package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

  @GetMapping("/index")
  public ModelAndView home() {
    return new ModelAndView("index");
  }

  @GetMapping("/")
  public ModelAndView index() {
    return new ModelAndView("index");
  }

}
