package org.obiba.mica.web.interceptor;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class MicaConfigInterceptor implements HandlerInterceptor {

  private final MicaConfigService micaConfigService;

  private MicaConfig micaConfig;

  @Inject
  public MicaConfigInterceptor(MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    if (modelAndView == null) return;
    ensureMicaConfig();
    modelAndView.getModel().put("config", micaConfig);
  }

  private void ensureMicaConfig() {
    synchronized (this) {
      if (micaConfig == null) {
        micaConfig = micaConfigService.getConfig();
      }
    }
  }

  public void evict() {
    synchronized (this) {
      micaConfig = null;
    }
  }
}
