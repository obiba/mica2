package org.obiba.mica.config.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * SpringBoot Actuator HealthIndicator check for the Database.
 */
public class DatabaseHealthCheckIndicator extends HealthCheckIndicator {

  public static final String DATABASE_HEALTH_INDICATOR = "database";

  private static final Logger log = LoggerFactory.getLogger(DatabaseHealthCheckIndicator.class);

  private MongoTemplate mongoTemplate;

  public DatabaseHealthCheckIndicator() {
  }

  public void setMongoTemplate(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  protected String getHealthCheckIndicatorName() {
    return DATABASE_HEALTH_INDICATOR;
  }

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
