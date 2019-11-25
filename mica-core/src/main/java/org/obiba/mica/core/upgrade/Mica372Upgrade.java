package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class Mica372Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica372Upgrade.class);


  private final MongoTemplate mongoTemplate;

  private final EventBus eventBus;

  private final TaxonomyConfigService taxonomyConfigService;

  @Inject
  public Mica372Upgrade(MongoTemplate mongoTemplate, EventBus eventBus, TaxonomyConfigService taxonomyConfigService) {
    this.mongoTemplate = mongoTemplate;
    this.eventBus = eventBus;
    this.taxonomyConfigService = taxonomyConfigService;
  }


  @Override
  public String getDescription() {
    return "Upgrade data to 3.7.2";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 7, 2);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.7.2");

    try {
      logger.info("Checking presence of \"start-range\" vocabulary in current Study taxonomy");
      mongoTemplate.execute(db -> db.eval(addStartRangeVocabularyToStudyTaxonomyIfMissing()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addStartRangeVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      logger.info("Indexing Study Taxonomy");
      Taxonomy studyTaxonomy = taxonomyConfigService.findByTarget(TaxonomyTarget.STUDY);
      eventBus.post(new TaxonomiesUpdatedEvent(studyTaxonomy.getName(), TaxonomyTarget.STUDY));
    } catch(Exception e) {
      logger.error("Failed to index Taxonomy", e);
    }
  }

  private String addStartRangeVocabularyToStudyTaxonomyIfMissing() {
    return
      "var startRangeVocabulary = {\n" +
        "  \"repeatable\": false,\n" +
        "  \"terms\": [\n" +
        "    {\n" +
        "      \"name\": \"*:1900\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"Before 1900\",\n" +
        "        \"fr\": \"Avant 1900\"\n" +
        "      },\n" +
        "      \"description\": {},\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1900:1950\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1900 to 1950\",\n" +
        "        \"fr\": \"1900 à 1950\"\n" +
        "      },\n" +
        "      \"description\": {},\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1950:2000\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1950 to 2000\",\n" +
        "        \"fr\": \"1950 à 2000\"\n" +
        "      },\n" +
        "      \"description\": {},\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"2000:2010\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"2000 to 2010\",\n" +
        "        \"fr\": \"2000 à 2010\"\n" +
        "      },\n" +
        "      \"description\": {},\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"2010:*\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"2010 and later\",\n" +
        "        \"fr\": \"2010 et plus tard\"\n" +
        "      },\n" +
        "      \"description\": {},\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    }\n" +
        "  ],\n" +
        "  \"name\": \"start-range\",\n" +
        "  \"title\": {\n" +
        "    \"en\": \"Start year (ranges)\",\n" +
        "    \"fr\": \"Année de début (intervalles)\"\n" +
        "  },\n" +
        "  \"description\": {\n" +
        "    \"en\": \"Year in which the study has started.\",\n" +
        "    \"fr\": \"Année à laquelle l'étude a commencé.\"\n" +
        "  },\n" +
        "  \"keywords\": {},\n" +
        "  \"attributes\": {\n" +
        "    \"type\": \"integer\",\n" +
        "    \"field\": \"model.startYear\",\n" +
        "    \"alias\": \"model-startYear-range\",\n" +
        "    \"range\": \"true\"\n" +
        "  }\n" +
        "};\n" +
        "\n" +
        "if (db.getCollection('taxonomyEntityWrapper').find({\n" +
        "    $and: [\n" +
        "        {_id: 'study', \"taxonomy.vocabularies\": {$elemMatch: {\"name\": \"start\"}}},\n" +
        "        {_id: 'study', \"taxonomy.vocabularies\": {$elemMatch: {\"attributes.field\": \"model.startYear\"}}}\n" +
        "    ]\n" +
        " }).count() > 0) {\n" +
        "    db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": startRangeVocabulary}})\n" +
        "}\n";
  }
}
