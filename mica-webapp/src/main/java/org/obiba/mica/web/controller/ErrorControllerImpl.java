package org.obiba.mica.web.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorControllerImpl implements ErrorController {

  @GetMapping("/error")
  public ModelAndView error(HttpServletRequest request, @RequestParam(value = "error", required = false) String status, @RequestParam(defaultValue = "") String message) {
    if (status == null) {
      // forwarded by the servlet container (unknown path, uncaught exception)
      Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
      status = code == null ? "500" : code.toString();
    }
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

  /**
   * Values are raw (they can come from the request): error.ftl escapes them when printed.
   */
  private ModelAndView makeModelAndView(String status, String message) {
    ModelAndView mv = new ModelAndView("error");
    mv.getModel().put("status", status);
    mv.getModel().put("msg", message);
    return mv;
  }
}
