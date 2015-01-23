package org.obiba.mica.config.metrics;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * SpringBoot Actuator HealthIndicator check for the Database.
 */
@Component
public class DatabaseHealthIndicator extends HealthCheckIndicator {

  private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Override
  protected Result check() {
    log.debug("Initializing Database health indicator");
    try {
      if(mongoTemplate.getDb().getStats().ok()) {
        return healthy();
      }
      return unhealthy("Cannot connect to database.");
    } catch(Exception e) {
      log.debug("Cannot connect to Database.", e);
      return unhealthy("Cannot connect to database.", e);
    }
  }
}
