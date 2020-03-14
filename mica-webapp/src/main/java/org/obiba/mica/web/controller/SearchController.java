package org.obiba.mica.web.controller;

import com.googlecode.protobuf.format.JsonFormat;
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
public class SearchController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @GetMapping("/search")
  public ModelAndView search(@CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                             @RequestParam(value = "language", required = false) String language) {
    ModelAndView mv = new ModelAndView("search");
    mv.getModel().put("configJson", getMicaConfigAsJson(language == null ? locale : language));
    return mv;
  }

  private String getMicaConfigAsJson(String language) {
    try {
      Mica.MicaConfigDto dto = dtos.asDto(micaConfigService.getConfig(), language);
      // TODO set the search UI translations instead
      dto = dto.toBuilder().clearTranslations().build();
      return JsonFormat.printToString(dto);
    } catch (Exception ignore) {
    }
    return "{}";
  }
}
