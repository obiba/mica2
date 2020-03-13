package org.obiba.mica.web.controller;

import com.googlecode.protobuf.format.JsonFormat;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;

@Controller
public class SearchController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @GetMapping("/search")
  public ModelAndView search() {
    ModelAndView mv = new ModelAndView("search");
    mv.getModel().put("configJson", getMicaConfigAsJson());
    return mv;
  }

  private String getMicaConfigAsJson() {
    try {
      Mica.MicaConfigDto dto = dtos.asDto(micaConfigService.getConfig());
      return JsonFormat.printToString(dto);
    } catch (Exception ignore) {
    }
    return "{}";
  }
}
