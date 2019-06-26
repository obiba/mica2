package org.obiba.mica.core.service;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Service
public class AgateServerConfigService {

  private String agateUrl;

  private String serviceName;

  private String serviceKey;

  protected final Environment env;

  @Inject
  public AgateServerConfigService(Environment env) {
    this.env = env;
  }

  @PostConstruct
  public void initialize() {
    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "agate.");
    agateUrl = propertyResolver.getProperty("url");
    serviceName = propertyResolver.getProperty("application.name");
    serviceKey = propertyResolver.getProperty("application.key");
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
