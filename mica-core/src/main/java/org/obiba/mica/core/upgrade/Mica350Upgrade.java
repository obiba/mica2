package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Mica350Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica350Upgrade.class);

  private MongoTemplate mongoTemplate;

  private EventBus eventBus;

  public Mica350Upgrade(
    MongoTemplate mongoTemplate,
    EventBus eventBus) {
    this.mongoTemplate = mongoTemplate;
    this.eventBus = eventBus;
  }

  @Override
  public String getDescription() {
    return "Upgrade data to 3.5.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 5, 0);
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("Executing Mica upgrade to version 3.5.0");


    try {
      logger.info("Removing old \"sets\" vocabulary in current Mica_variable taxonomy");
      mongoTemplate.execute(db -> db.eval(removeSetsVocabulariesTerms()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to execute removeSetsVocabulariesTerms.", e);
    }


    try {
      logger.info("Indexing Taxonomies");
      eventBus.post(new TaxonomiesUpdatedEvent());
    } catch(Exception e) {
      logger.error("Failed to index Taxonomies", e);
    }
  }

  private String removeSetsVocabulariesTerms() {
    return
      "var setsVocabulary = {\n" +
        "                \"name\" : \"sets\",\n" +
        "                \"title\" : {\n" +
        "                    \"en\" : \"Sets\",\n" +
        "                    \"fr\" : \"Ensembles\"\n" +
        "                },\n" +
        "                \"description\" : {\n" +
        "                    \"en\" : \"Sets in which the variable appears.\",\n" +
        "                    \"fr\" : \"Ensembles dans lesquels est définie la variable.\"\n" +
        "                },\n" +
        "                \"keywords\" : {},\n" +
        "                \"repeatable\" : false,\n" +
        "                \"terms\" : [],\n" +
        "                \"attributes\" : {\n" +
        "                    \"static\" : \"true\",\n" +
        "                    \"hidden\" : \"true\"\n" +
        "                }\n" +
        "            };\n" +
        "\n" +
        "db.taxonomyEntityWrapper.update({\"_id\": \"variable\"}, {$pull: {\"taxonomy.vocabularies\": {name: \"sets\"}}});\n" +
        "db.taxonomyEntityWrapper.update({\"_id\": \"variable\"}, {$push: {\"taxonomy.vocabularies\": setsVocabulary}});\n";
  }
}
