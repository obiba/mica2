package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ErrorController {

  @GetMapping("/error")
  public ModelAndView error() {
    return new ModelAndView("error");
  }

  @PostMapping(value = "/error", consumes = "application/x-www-form-urlencoded")
  public ModelAndView errorWithParams(@RequestParam(defaultValue = "500") int status, @RequestParam(defaultValue = "") String message) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("status", status);
    params.put("msg", message);
    return new ModelAndView("error", params);
  }

}
