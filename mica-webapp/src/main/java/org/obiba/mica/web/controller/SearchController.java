package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SearchController {

  @GetMapping("/search")
  public ModelAndView search() {
    return new ModelAndView("search");
  }

}
