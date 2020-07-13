package org.obiba.mica.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ErrorController implements org.springframework.boot.autoconfigure.web.ErrorController {

  @GetMapping("/error")
  public ModelAndView error(@RequestParam(value = "error", required = false, defaultValue = "500") String status, @RequestParam(defaultValue = "") String message) {
    return makeModelAndView(status, message);
  }

  @PostMapping(value = "/error", consumes = "application/x-www-form-urlencoded")
  public ModelAndView errorWithParams(@RequestParam(defaultValue = "500") int status, @RequestParam(defaultValue = "") String message) {
    return makeModelAndView(status + "", message);
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView anyError(Exception ex) {
    return makeModelAndView("500", ex.getMessage());
  }

  @Override
  public String getErrorPath() {
    return "/error";
  }

  private ModelAndView makeModelAndView(String status, String message) {
    ModelAndView mv = new ModelAndView("error");
    mv.getModel().put("status", status);
    mv.getModel().put("msg", message);
    return mv;
  }
}
