package org.obiba.mica.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Map;

@Controller
public class SearchController {

  private MicaConfigService micaConfigService;

  @Inject
  public SearchController(MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  @GetMapping("/search")
  public ModelAndView search() {
    Map<String, Object> params = Maps.newHashMap();
    params.put("configJson", getMicaConfigAsJson());
    return new ModelAndView("search", params);
  }

  private String getMicaConfigAsJson() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(micaConfigService.getConfig());
    } catch (JsonProcessingException ignore) {
    }

    return "{}";
  }

}
