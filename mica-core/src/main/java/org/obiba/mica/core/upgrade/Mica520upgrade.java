package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.json.JSONException;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Mica520upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica520upgrade.class);

  private final CacheService cacheService;

  private final MongoTemplate mongoTemplate;

  private final PluginsService pluginsService;

  @Inject
  public Mica520upgrade(CacheService cacheService, MongoTemplate mongoTemplate, PluginsService pluginsService) {
    this.cacheService = cacheService;
    this.mongoTemplate = mongoTemplate;
    this.pluginsService = pluginsService;
  }

  @Override
  public String getDescription() {
    return "Clear caches, update config and search plugin for 5.2.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(5, 1, 0);
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("Executing Mica upgrade to version 5.2.0");

    try {
      logger.info("Updating 'Mica Config'...");
      updateMicaConfig();
    } catch (JSONException e) {
      logger.error("Error occurred while Updating 'Mica Config'");
    }

    logger.info("Clearing all caches...");
    cacheService.clearAllCaches();

    logger.info("Updating search plugin...");
    try {
      updateSearchPlugin();
      // TODO rebuild search index?
    } catch (Exception e) {
      // not installed, not to be upgraded
      // OR upgrade failed for unknown reason
      logger.error("Error occurred while updating 'Search Plugin' to its latest version", e);
    }
  }

  private void updateMicaConfig() throws JSONException {
    Document micaConfig = getDBObjectSafely("micaConfig");
    // delete field opal as it was not used
    if (null != micaConfig) {
      micaConfig.remove("opal");
      mongoTemplate.save(micaConfig, "micaConfig");
    }
  }

  private void updateSearchPlugin() {
    String searchPluginName = pluginsService.getSearchPluginName();
    // check it is installed
    pluginsService.getInstalledPlugin(searchPluginName);
    // check it is updatable
    if (pluginsService.getUpdatablePlugins().stream().anyMatch(pkg -> pkg.getName().equals(searchPluginName))) {
      // install latest version
      pluginsService.installPlugin(searchPluginName, null);
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
