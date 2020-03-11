package org.obiba.mica.web.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MicaConfigInterceptor extends HandlerInterceptorAdapter {

  private static final Logger log = LoggerFactory.getLogger(MicaConfigInterceptor.class);

  private final MicaConfigService micaConfigService;

  private MicaConfig micaConfig;

  private String micaConfigJson;

  @Inject
  public MicaConfigInterceptor(MicaConfigService configService) {
    this.micaConfigService = configService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    ensureMicaConfig();
    modelAndView.getModel().put("config", micaConfig);
    modelAndView.getModel().put("configJson", micaConfigJson);
  }

  private void ensureMicaConfig() {
    synchronized (this) {
      if (micaConfig == null) {
        micaConfig = micaConfigService.getConfig();
        micaConfigJson = getMicaConfigAsJson();
      }
    }
  }

  private String getMicaConfigAsJson() {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.writeValueAsString(micaConfig).replaceAll("\"", "\\\\\"");
    } catch (JsonProcessingException ignore) {
    }

    return "{}";
  }

  public void evict() {
    synchronized (this) {
      micaConfig = null;
    }
  }
}
