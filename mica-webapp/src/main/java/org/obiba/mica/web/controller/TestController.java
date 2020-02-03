package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class TestController extends BaseController {

  @GetMapping("/test")
  public ModelAndView get() {
    Map<String, Object> params = newParameters();
    return new ModelAndView("test", params);
  }

}
