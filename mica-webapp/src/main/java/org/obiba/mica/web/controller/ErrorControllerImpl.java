package org.obiba.mica.web.controller;

import org.owasp.esapi.ESAPI;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
@Controller
public class ErrorControllerImpl implements ErrorController {

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

  private ModelAndView makeModelAndView(String status, String message) {
    ModelAndView mv = new ModelAndView("error");
    mv.getModel().put("status", ESAPI.encoder().encodeForHTML(status));
    mv.getModel().put("msg", ESAPI.encoder().encodeForHTML(message));
    return mv;
  }

  public String getErrorPath() {
    return "/error";
  }
}
