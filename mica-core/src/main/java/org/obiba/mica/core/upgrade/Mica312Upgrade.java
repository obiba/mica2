package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Mica312Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  private static final Logger logger = LoggerFactory.getLogger(Mica312Upgrade.class);

  @Override
  public String getDescription() {
    return "Upgrade data to 3.1.2";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 1, 2);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.1.2");

    logger.info("Updating variable's variableType vocabulary terms.");
    mongoTemplate.execute(db -> db.eval(variableTypeVocabularyTermsUpdate()));
  }

  private String variableTypeVocabularyTermsUpdate() {
    return
      "var variableTypeVocabularyTerms = [\n" +
        "  {\n" + "    \"name\": \"Collected\",\n" +
        "    \"title\": {\n" +
        "      \"en\": \"Collected\",\n" +
        "      \"fr\": \"Collecte\"\n" +
        "    },\n" +
        "    \"description\": {\n" +
        "      \"en\": \"A collection variable holds metadata about data collected within a data collection event.\",\n" +
        "      \"fr\": \"Variables d'un jeux de données spécifique à une étude de collecte.\"\n" +
        "    },\n" +
        "    \"keywords\": {},\n" +
        "    \"attributes\": {}\n" +
        "  },\n" +
        "  {\n" +
        "    \"name\": \"Dataschema\",\n" +
        "    \"title\": {\n" +
        "      \"en\": \"DataSchema\",\n" +
        "      \"fr\": \"Schéma de données\"\n" +
        "    },\n" +
        "    \"description\": {\n" +
        "      \"en\": \"A dataschema variable is part of a harmonization dataset and is a construct from multiple study variables.\"\n" +
        "    },\n" +
        "    \"keywords\": {},\n" +
        "    \"attributes\": {}\n" +
        "  }\n" +
        "];\n" +
        "\n" +
        "db.taxonomyEntityWrapper.update(\n" +
        "  {\"_id\": \"variable\", \"taxonomy.vocabularies\": {$elemMatch: {\"name\": \"variableType\"}}}, \n" +
        "  {$set: {\"taxonomy.vocabularies.$.terms\": variableTypeVocabularyTerms}})";
  }
}
