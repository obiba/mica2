package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.googlecode.protobuf.format.JsonFormat;
import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.support.SpecialCharCodecFactory;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class SearchController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(SearchController.class);
  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @GetMapping("/search")
  public ModelAndView search(@CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language, String searchContext) {
    MicaConfig config = micaConfigService.getConfig();
    Subject subject = SecurityUtils.getSubject();
    if (!config.isOpenAccess() && !subject.isAuthenticated()) {
      String searchPage = Strings.isNullOrEmpty(searchContext) ? "search" : String.format("%s-search", searchContext);
      return new ModelAndView("redirect:/signin?redirect=" + micaConfigService.getContextPath() + "/" + searchPage);
    } else {
      ModelAndView mv = new ModelAndView("search");
      Map<String, Object> model = mv.getModel();
      model.put("searchContext", Strings.isNullOrEmpty(searchContext) ? "" : searchContext);
      model.put("configJson", getMicaConfigAsJson(config, getLang(locale, language)));
      model.put("specialCharCodec", SpecialCharCodecFactory.get().getEncodeMap());
      return mv;
    }
  }

  @GetMapping("/individual-search")
  public ModelAndView individualSearch(@CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    return search(locale, language, "individual");
  }

  @GetMapping("/harmonization-search")
  public ModelAndView harmonizationSearch(@CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                          @RequestParam(value = "language", required = false) String language) {
    return search(locale, language, "harmonization");
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
