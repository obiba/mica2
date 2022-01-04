package org.obiba.mica.core.upgrade;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;

@Component
public class Mica470Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica470Upgrade.class);

  private MongoTemplate mongoTemplate;

  public Mica470Upgrade(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public String getDescription() {
    return "Upgrade taxonomies for 4.7.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(4, 7, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 4.7.0");

    if (mongoTemplate.collectionExists("taxonomyEntityWrapper")) {
      BasicDBObject dbObject = BasicDBObject.parse("{_id: \"study\"}");

      List<String> exclusiveStudyVocabularyNames = Arrays.asList("objectives", "methods-design", "populations-selectionCriteria-countriesIso",
        "populations-id", "populations-name", "populations-description", "populations-selectionCriteria-ageMin",
        "populations-selectionCriteria-ageMax", "populations-selectionCriteria-gender",
        "populations-selectionCriteria-pregnantWomen", "populations-selectionCriteria-newborn",
        "populations-selectionCriteria-twins", "numberOfParticipants-participant-number",
        "numberOfParticipants-participant-range", "numberOfParticipants-sample-number",
        "numberOfParticipants-sample-range", "methods-recruitments", "populations-recruitment-dataSources",
        "populations-dataCollectionEvents-dataSources", "populations-recruitment-generalPopulationSources",
        "populations-recruitment-specificPopulationSources", "populations-dataCollectionEvents-bioSamples",
        "populations-dataCollectionEvents-administrativeDatabases", "access_data", "access_bio_samples", "access_other",
        "populations-dataCollectionEvents-id", "populations-dataCollectionEvents-name",
        "populations-dataCollectionEvents-start", "populations-dataCollectionEvents-end");

      BasicDBObject arrayFilter = BasicDBObject.parse("{ \"elem.name\": { $in: " + new JSONArray(exclusiveStudyVocabularyNames) + " } }");

      mongoTemplate.getCollection("taxonomyEntityWrapper").initializeOrderedBulkOperation()
        .find(dbObject)
        .arrayFilters(Arrays.asList(arrayFilter))
        .update(BasicDBObject.parse("{ $set: { \"taxonomy.vocabularies.$[elem].attributes.forClassName\": \"Study\" } }"));

        logger.info("Added forClassName attribute for exclusive Study vocabularies");
    }
  }
}
