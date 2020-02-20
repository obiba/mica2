package org.obiba.mica.web.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
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

  @GetMapping("/admin")
  public ModelAndView admin() {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      return new ModelAndView("admin");
    } else {
      return new ModelAndView("redirect:signin?redirect=admin");
    }
  }

}
