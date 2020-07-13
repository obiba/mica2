package org.obiba.mica.web.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.service.UserAuthService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.controller.domain.AuthConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;

@Controller
public class ProfileController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private UserAuthService userAuthService;

  @GetMapping("/profile")
  public ModelAndView getProfile() {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      ModelAndView mv = new ModelAndView("profile");
      mv.getModel().put("oidcProviders", userAuthService.getOidcProviders("en"));
      mv.getModel().put("authConfig", new AuthConfiguration(userAuthService.getPublicConfiguration(), null));
      return mv;
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/profile");
    }
  }

  @GetMapping("/forgot-password")
  public ModelAndView forgotPassword() {
    ModelAndView mv = new ModelAndView("forgot-password");
    return mv;
  }

}
