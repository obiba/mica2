package org.obiba.mica.core.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;

import java.net.URI;

@Service
public class AgateServerConfigService implements InitializingBean {

  private String agateUrlString;

  private URI agateUri;

  private String serviceName;

  private String serviceKey;

  protected final Environment env;

  @Inject
  public AgateServerConfigService(Environment env) {
    this.env = env;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    agateUrlString = env.getProperty("agate.url", "http://localhost:8081");
    agateUri = URI.create(agateUrlString);
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
    return agateUrlString;
  }

  public URI getAgateUri() {
    return agateUri;
  }

  public String buildToken() {
    return serviceName + ":" + serviceKey;
  }

  public boolean isSecured() {
    return agateUrlString.toLowerCase().startsWith("https://");
  }
}
