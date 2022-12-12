package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.json.JSONException;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Mica510upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica510upgrade.class);

  private CacheService cacheService;

  private final MongoTemplate mongoTemplate;

  @Inject
  public Mica510upgrade(CacheService cacheService, MongoTemplate mongoTemplate) {
    this.cacheService = cacheService;
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public String getDescription() {
    return "Clear caches and updating config for 5.1.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(5, 1, 0);
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("Executing Mica upgrade to version 5.1.0");

    try {
      logger.info("Updating 'Mica Config'...");
      updateMicaConfig();
    } catch (JSONException e) {
      logger.error("Error occurred while Updating 'Mica Config'");
    }

    logger.info("Clearing all caches...");
    cacheService.clearAllCaches();

    // TODO get updated search plugin and rebuild search index
  }

  private void updateMicaConfig() throws JSONException {
    Document micaConfig = getDBObjectSafely("micaConfig");
    // delete field anonymousCanCreateCart to reset to default
    if (null != micaConfig) {
      micaConfig.remove("opal");
      mongoTemplate.save(micaConfig, "micaConfig");
    }
  }

  private Document getDBObjectSafely(String collectionName) {
    if (mongoTemplate.collectionExists(collectionName)) {
      MongoCollection<Document> existingCollection = mongoTemplate.getCollection(collectionName);
      return existingCollection.find().first();
    }

    return null;
  }

}
