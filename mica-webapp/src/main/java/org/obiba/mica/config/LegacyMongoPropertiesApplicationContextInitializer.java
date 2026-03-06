package org.obiba.mica.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * ApplicationContextInitializer that maps the old Spring Boot 2 property
 * spring.data.mongodb.uri (and common variants) to the new
 * spring.mongodb.uri property introduced in newer Spring Boot versions.
 *
 * This keeps deployments using the old property working after an upgrade.
 */
public class LegacyMongoPropertiesApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final Logger log = LoggerFactory.getLogger(LegacyMongoPropertiesApplicationContextInitializer.class);

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    log.info("Checking for legacy MongoDB properties to ensure compatibility with newer Spring Boot versions.");
    ConfigurableEnvironment environment = applicationContext.getEnvironment();
    final String newKey = "spring.mongodb.uri";

    // Common legacy keys used across older Spring Boot versions/configs
    final String[] legacyKeys = new String[] {
        "spring.data.mongodb.uri",
        "spring.data.mongodb.url",
    };

    // If new property already set, do nothing
    if(environment.getProperty(newKey) != null) {
      return;
    }

    for(String legacyKey : legacyKeys) {
      String value = environment.getProperty(legacyKey);
      if(value != null && !value.isEmpty()) {
        Map<String, Object> map = new HashMap<>();
        map.put(newKey, value);
        MutablePropertySources sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource("legacyMongoProperties", map));
        log.warn("Deprecated property '{}' detected. Mapping it to '{}' so upgrade remains compatible.", legacyKey, newKey);
        break;
      }
    }
  }
}
