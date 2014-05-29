package org.obiba.mica.config;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

@Configuration
public class ThymeleafConfiguration {

  @Inject
  private ThymeleafViewResolver thymeleafViewResolver;

  @PostConstruct
  private void init() {
    thymeleafViewResolver.setViewNames(new String[] { "error", "/tl/*" });
  }
}
