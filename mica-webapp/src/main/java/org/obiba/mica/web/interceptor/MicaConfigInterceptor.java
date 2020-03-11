package org.obiba.mica.web.interceptor;

import com.googlecode.protobuf.format.JsonFormat;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
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

  private final Dtos dtos;

  private final MicaConfigService micaConfigService;

  private MicaConfig micaConfig;

  private String micaConfigJson;

  @Inject
  public MicaConfigInterceptor(Dtos dtos, MicaConfigService micaConfigService) {
    this.dtos = dtos;
    this.micaConfigService = micaConfigService;
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
    try {
      Mica.MicaConfigDto dto = dtos.asDto(micaConfig);
      return JsonFormat.printToString(dto);
    } catch (Exception ignore) {
    }
    return "{}";
  }

  public void evict() {
    synchronized (this) {
      micaConfig = null;
    }
  }
}
