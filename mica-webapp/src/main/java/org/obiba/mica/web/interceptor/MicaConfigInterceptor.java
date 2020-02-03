package org.obiba.mica.web.interceptor;

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

  @Inject
  public MicaConfigInterceptor(MicaConfigService configService) {
    this.micaConfigService = configService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    modelAndView.getModel().put("config", getMicaConfig());
  }

  private MicaConfig getMicaConfig() {
    synchronized (this) {
      if (micaConfig == null)
        micaConfig = micaConfigService.getConfig();
      return micaConfig;
    }
  }

  public void evict() {
    synchronized (this) {
      micaConfig = null;
    }
  }
}
