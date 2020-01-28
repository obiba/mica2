package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
public class CatalogController {

  @GetMapping("/catalog")
  public ModelAndView get() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("message", "coucou");

    return new ModelAndView("catalog", params);
  }

}
