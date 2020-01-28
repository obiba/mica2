package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DataAccessController {

  @GetMapping("/data-access/{id}")
  public ModelAndView get(@PathVariable String id) {
    return new ModelAndView("data-access");
  }

  @GetMapping("/data-access/{id}/form")
  public ModelAndView getForm(@PathVariable String id) {
    return new ModelAndView("data-access-form");
  }

  @GetMapping("/data-access/{id}/amendment/{aid}/form")
  public ModelAndView getAmendmentForm(@PathVariable String id, @PathVariable String aid) {
    return new ModelAndView("data-access-amendment-form");
  }

  @GetMapping("/data-access/{id}/comments")
  public ModelAndView getComments(@PathVariable String id) {
    return new ModelAndView("data-access-comments");
  }

  @GetMapping("/data-access/{id}/history")
  public ModelAndView getHistory(@PathVariable String id) {
    return new ModelAndView("data-access-history");
  }

}
