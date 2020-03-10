package org.obiba.mica.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Map;

@Controller
public class SearchController {

  private MicaConfigService micaConfigService;

  private String cachedMicaConfigJson = null;

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

  @Async
  @Subscribe
  public void micaConfigUpdated(MicaConfigUpdatedEvent event) {
    cachedMicaConfigJson = null;
    initMicaConfigAsJson();
  }

  private void initMicaConfigAsJson() {
    if (cachedMicaConfigJson != null) return;

    ObjectMapper mapper = new ObjectMapper();
    try {
      cachedMicaConfigJson = mapper.writeValueAsString(micaConfigService.getConfig());
    } catch (JsonProcessingException ignore) {
    }
  }

  private String getMicaConfigAsJson() {
    return cachedMicaConfigJson == null ? "{}": cachedMicaConfigJson;
  }
}
