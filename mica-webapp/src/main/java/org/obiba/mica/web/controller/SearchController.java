package org.obiba.mica.web.controller;

import com.googlecode.protobuf.format.JsonFormat;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;

@Controller
public class SearchController extends BaseController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @GetMapping("/search")
  public ModelAndView search(@CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    MicaConfig config = micaConfigService.getConfig();
    Subject subject = SecurityUtils.getSubject();
    if (!config.isOpenAccess() && !subject.isAuthenticated()) {
      return new ModelAndView("redirect:/signin?redirect=" + micaConfigService.getContextPath() + "/search");
    } else {
      ModelAndView mv = new ModelAndView("search");
      mv.getModel().put("configJson", getMicaConfigAsJson(config, getLang(locale, language)));
      return mv;
    }
  }

  private String getMicaConfigAsJson(MicaConfig config, String language) {
    try {
      Mica.MicaConfigDto dto = dtos.asDto(config, language);
      // TODO set the search UI translations instead
      dto = dto.toBuilder().clearTranslations().build();
      return JsonFormat.printToString(dto);
    } catch (Exception ignore) {
    }
    return "{}";
  }
}
