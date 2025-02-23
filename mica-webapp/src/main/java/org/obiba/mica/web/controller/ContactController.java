package org.obiba.mica.web.controller;

import org.json.JSONObject;
import org.obiba.mica.core.service.UserAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;

@Controller
public class ContactController extends BaseController {

  @Inject
  private UserAuthService userAuthService;

  @GetMapping("/contact")
  public ModelAndView contact() {
    ModelAndView mv = new ModelAndView("contact");
    mv.getModel().put("reCaptchaKey", getReCaptchaKey());
    return mv;
  }

  private String getReCaptchaKey() {
    try {
      JSONObject clientConfig = userAuthService.getClientConfiguration();
      return clientConfig.getString("reCaptchaKey");
    } catch (Exception e) {
      return "";
    }
  }

}
