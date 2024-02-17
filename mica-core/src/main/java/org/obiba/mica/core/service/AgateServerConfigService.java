package org.obiba.mica.core.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class AgateServerConfigService implements InitializingBean {

  private String agateUrl;

  private String serviceName;

  private String serviceKey;

  protected final Environment env;

  @Inject
  public AgateServerConfigService(Environment env) {
    this.env = env;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    agateUrl = env.getProperty("agate.url");
    serviceName = env.getProperty("agate.application.name");
    serviceKey = env.getProperty("agate.application.key");
  }

  public String getServiceKey() {
    return serviceKey;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getAgateUrl() {
    return agateUrl;
  }

  public String buildToken() {
    return serviceName + ":" + serviceKey;
  }

  public boolean isSecured() {
    return agateUrl.toLowerCase().startsWith("https://");
  }
}
