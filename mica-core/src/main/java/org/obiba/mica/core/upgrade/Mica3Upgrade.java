/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Mica3Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica3Upgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Override
  public String getDescription() {
    return "Mica 3.0.0 upgrade";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 0, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.0.0");

    try {
      executeMongoScript(renameDocuments(),
        "Replacing all references to /study by /individual-study and /study-dataset by /collected-dataset and harmonization-dataset by harmonized-dataset...");
    } catch (RuntimeException e) {
      logger.error("Error occurred when updating Study path resources (/study -> /individual-study, /study-dataset -> /collected-dataset and /harmonization-dataset -> /harmonized-dataset).", e);
    }

    try {
      executeMongoScript(addClassNameVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"className\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addClassNameVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addHarmonizationDesignVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"harmonizationDesign\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addHarmonizationDesignVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsIdVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-id\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsIdVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsNameVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-name\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsNameVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsDescriptionVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-description\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsDescriptionVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsDataCollectionEventsIdVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-dataCollectionEvents-id\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsDataCollectionEventsIdVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsDataCollectionEventsNameVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-dataCollectionEvents-name\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsDataCollectionEventsNameVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsDataCollectionEventsStartVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-dataCollectionEvents-start\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsDataCollectionEventsStartVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsDataCollectionEventsEndVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-dataCollectionEvents-end\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsDataCollectionEventsEndVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationsDataCollectionEventsDescriptionVocabularyToStudyTaxonomyIfMissing(),
        "Checking presence of \"populations-dataCollectionEvents-description\" vocabulary in current Mica_study taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationsDataCollectionEventsDescriptionVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addStudyIdVocabularyToVariableTaxonomyIfMissing(),
        "Checking presence of \"studyId\" vocabulary in current Mica_variable taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addStudyIdVocabularyToVariableTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addPopulationIdVocabularyToVariableTaxonomyIfMissing(),
        "Checking presence of \"populationId\" vocabulary in current Mica_variable taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationIdVocabularyToVariableTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(addDceIdVocabularyToVariableTaxonomyIfMissing(),
        "Checking presence of \"dceId\" vocabulary in current Mica_variable taxonomy");
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addDceIdVocabularyToVariableTaxonomyIfMissing.", e);
    }

    try {
      executeMongoScript(removeUndFromTaxonomyVocabularyFieldAndAliasValuesWhileAlsoSettingStaticAttributeToTrue(),
        "Removing \"und\" from taxonomy vocabulary attribute field and alias");
    } catch(RuntimeException e) {
      logger.error("Error occurred when trying to removeUndFromTaxonomyVocabularyFieldAndAlias.", e);
    }

    try {
      executeMongoScript(updateDatasetClassNameVocabularyDescriptionAndTermTitleAndTermDescription(),
        "Updating dataset taxonomy vocabulary \"className\" such that its description and term title and description don't use \"harmonization\" or \"study\".");
    } catch(RuntimeException e) {
      logger.error("Error occurred when trying to updateDatasetClassNameVocabularyDescriptionAndTermTitleAndTermDescription.", e);
    }

    unpublishAllHarmonizationDataset();
  }

  private void unpublishAllHarmonizationDataset() {
    harmonizationDatasetStateRepository.findByPublishedTagNotNull().forEach(
      harmonizationDatasetState -> harmonizedDatasetService.unPublishState(harmonizationDatasetState.getId())
    );
  }

  private void executeMongoScript(String script, String infoLog) {
    logger.info(infoLog);
    mongoTemplate.execute(db -> db.eval(script));
  }

  private String renameDocuments() {
    return
      "function bulkUpdateAttachmentPath(collection, fields, regexp, replace) {\n" +
        "    var bulk = collection.initializeOrderedBulkOp();\n" +
        "    fields.forEach(function (field) {\n" +
        "        var findQuery = {};\n" +
        "        findQuery[field] = regexp;\n" +
        "        collection.find(findQuery).forEach(function (doc) {\n" +
        "            var replaceQuery = {};\n" +
        "            replaceQuery[field] = doc[field].replace(regexp, replace);\n" +
        "            bulk.find({\"_id\": doc._id}).updateOne({\"$set\": replaceQuery});\n" +
        "        });\n" +
        "    });\n" +
        "    bulk.execute();\n" +
        "};\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/study-dataset/, '/collected-dataset');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/study-dataset/, '/collected-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study-dataset$/, '/collected-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study-dataset\\//, '/collected-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study-dataset$/, '/draft/collected-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study-dataset\\//, '/draft/collected-dataset/');\n" +
        "\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/harmonization-dataset/, '/harmonized-dataset');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/harmonization-dataset/, '/harmonized-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/harmonization-dataset$/, '/harmonized-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/harmonization-dataset\\//, '/harmonized-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/harmonization-dataset$/, '/draft/harmonized-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/harmonization-dataset\\//, '/draft/harmonized-dataset/');\n" +
        "\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/study/, '/individual-study');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/study/, '/individual-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study$/, '/individual-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study\\//, '/individual-study/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study$/, '/draft/individual-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study\\//, '/draft/individual-study/');" +
        "";
  }

  private String addClassNameVocabularyToStudyTaxonomyIfMissing() {
    return
      "var classNameVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"terms\": [\n" +
      "    {\n" +
      "      \"name\": \"Study\",\n" +
      "      \"title\": {\"en\": \"Individual\", \"fr\": \"Individuelle\"},\n" +
      "      \"description\": {\n" +
      "        \"en\": \"An individual study.\",\n" +
      "        \"fr\": \"Une étude de collecte de données.\",\n" +
      "        \"terms\": [],\n" +
      "        \"keywords\": {},\n" +
      "        \"attributes\": {}\n" +
      "      }\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"HarmonizationStudy\",\n" +
      "      \"title\": {\"en\": \"Harmonization\", \"fr\": \"Harmonisation\"},\n" +
      "      \"description\": {\n" +
      "        \"en\": \"A harmonization study.\",\n" +
      "        \"fr\": \"Une étude d'harmonisation de données.\",\n" +
      "        \"terms\": [],\n" +
      "        \"keywords\": {},\n" +
      "        \"attributes\": {}\n" +
      "      }\n" +
      "    }\n" +
      "  ],\n" +
      "  \"name\": \"className\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Type of Study\",\n" +
      "    \"fr\": \"Type d'étude\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"Type of study (e.g. study or harmonization).\",\n" +
      "    \"fr\": \"Type de l'étude (par exemple, étude ou étude d'harmonisation).\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"static\": \"true\"\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"className\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": classNameVocabulary}});\n" +
      "}\n";
  }

  private String addHarmonizationDesignVocabularyToStudyTaxonomyIfMissing() {
    return
      "var harmonizationDesignVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"harmonizationDesign\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Harmonization Study Design\",\n" +
      "    \"fr\": \"Conception de l'étude d'harmonisation\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"Harmonization Study Design.\",\n" +
      "    \"fr\": \"Conception de l'étude d'harmonisation.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"model.harmonizationDesign\",\n" +
      "    \"localized\": \"true\",\n" +
      "    \"alias\": \"model-harmonizationDesign\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"harmonizationDesign\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": harmonizationDesignVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsIdVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsIdVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-id\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Population ID\",\n" +
      "    \"fr\": \"Identifiant d'une population\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A population's ID.\",\n" +
      "    \"fr\": \"L'identifiant d'une population.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"populations.id\",\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"alias\": \"populations-id\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-id\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsIdVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsNameVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsNameVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-name\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Population Name\",\n" +
      "    \"fr\": \"Nom d'une population\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A population's name.\",\n" +
      "    \"fr\": \"Le nom d'une population.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"populations.name\",\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"localized\": \"true\",\n" +
      "    \"alias\": \"populations-name\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-name\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsNameVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsDescriptionVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsDescriptionVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-description\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Population Description\",\n" +
      "    \"fr\": \"Description d'une population\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A population's description.\",\n" +
      "    \"fr\": \"La description d'une population.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"populations.description\",\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"alias\": \"populations-description\",\n" +
      "    \"localized\": \"true\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-description\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsDescriptionVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsDataCollectionEventsIdVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsDataCollectionEventsIdVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-dataCollectionEvents-id\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Data Collection Event ID\",\n" +
      "    \"fr\": \"Identifiant d'un événement de collecte de données\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A data collection event's ID.\",\n" +
      "    \"fr\": \"L'indentifiant d'un événement de collecte de données.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"field\": \"populations.dataCollectionEvents.id\",\n" +
      "    \"alias\": \"populations-dataCollectionEvents-id\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-dataCollectionEvents-id\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsDataCollectionEventsIdVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsDataCollectionEventsNameVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsDataCollectionEventsNameVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-dataCollectionEvents-name\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Data Collection Event Name\",\n" +
      "    \"fr\": \"Nom d'un événement de collecte de données\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A data collection event's name.\",\n" +
      "    \"fr\": \"Le nom d'un événement de collecte de données.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"localized\": \"true\",\n" +
      "    \"field\": \"populations.dataCollectionEvents.name\",\n" +
      "    \"alias\": \"populations-dataCollectionEvents-name\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-dataCollectionEvents-name\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsDataCollectionEventsNameVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsDataCollectionEventsStartVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsDataCollectionEventsStartVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-dataCollectionEvents-start\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Data Collection Event Start Date\",\n" +
      "    \"fr\": \"Date de début d'un événement de collecte de données\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A data collection event's initiation date.\",\n" +
      "    \"fr\": \"La date de commencement d'un événement de collecte de données.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"populations.dataCollectionEvents.start.yearMonth\",\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"alias\": \"populations-dataCollectionEvents-start-yearMonth\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-dataCollectionEvents-start\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsDataCollectionEventsStartVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsDataCollectionEventsEndVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsDataCollectionEventsEndVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-dataCollectionEvents-end\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Data Collection Event End Date\",\n" +
      "    \"fr\": \"Date de fin d'un événement de collecte de données\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A data collection event's end date.\",\n" +
      "    \"fr\": \"La date de fin d'un événement de collecte de données.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"populations.dataCollectionEvents.end.yearMonth\",\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"alias\": \"populations-dataCollectionEvents-end-yearMonth\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-dataCollectionEvents-end\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsDataCollectionEventsEndVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationsDataCollectionEventsDescriptionVocabularyToStudyTaxonomyIfMissing() {
    return
      "var populationsDataCollectionEventsDescriptionVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populations-dataCollectionEvents-description\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Data Collection Event Description\",\n" +
      "    \"fr\": \"Description d'un événement de collecte de données\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"A data collection event's description.\",\n" +
      "    \"fr\": \"La description d'un événement de collecte de données.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"field\": \"populations.dataCollectionEvents.description\",\n" +
      "    \"static\": \"true\",\n" +
      "    \"hidden\": \"true\",\n" +
      "    \"localized\": \"true\",\n" +
      "    \"alias\": \"populations-dataCollectionEvents-description\",\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"study\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populations-dataCollectionEvents-description\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": populationsDataCollectionEventsDescriptionVocabulary}});\n" +
      "}\n";
  }

  private String addStudyIdVocabularyToVariableTaxonomyIfMissing() {
    return
      "var studyIdVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"studyId\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Study\",\n" +
      "    \"fr\": \"Étude\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"Study in which the variable appears.\",\n" +
      "    \"fr\": \"Étude dans laquelle est définie la variable.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"static\": \"true\"\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"variable\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"studyId\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"variable\"}, {$push: {\"taxonomy.vocabularies\": studyIdVocabulary}});\n" +
      "}\n";
  }

  private String addPopulationIdVocabularyToVariableTaxonomyIfMissing() {
    return
      "var populationIdVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"populationId\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Population\",\n" +
      "    \"fr\": \"Population\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"Population for which variable's data were collected.\",\n" +
      "    \"fr\": \"Population pour laquelle des données ont été collectées.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"static\": \"true\"\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"variable\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"populationId\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"variable\"}, {$push: {\"taxonomy.vocabularies\": populationIdVocabulary}});\n" +
      "}\n";
  }

  private String addDceIdVocabularyToVariableTaxonomyIfMissing() {
    return
      "var dceIdVocabulary = {\n" +
      "  \"repeatable\": false,\n" +
      "  \"name\": \"dceId\",\n" +
      "  \"title\": {\n" +
      "    \"en\": \"Data Collection Event\",\n" +
      "    \"fr\": \"Événement de collecte\"\n" +
      "  },\n" +
      "  \"description\": {\n" +
      "    \"en\": \"Event during which variable's data were collected.\",\n" +
      "    \"fr\": \"Événement durant lequel des données ont été collectées.\"\n" +
      "  },\n" +
      "  \"keywords\": {},\n" +
      "  \"attributes\": {\n" +
      "    \"static\": \"true\"\n" +
      "  }\n" +
      "};\n" +
      "\n" +
      "if (db.taxonomyEntityWrapper.find({\"_id\": \"variable\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"dceId\"}}}).count() === 0) {\n" +
      "  db.taxonomyEntityWrapper.update({\"_id\": \"variable\"}, {$push: {\"taxonomy.vocabularies\": dceIdVocabulary}});\n" +
      "}\n";
  }

  private String removeUndFromTaxonomyVocabularyFieldAndAliasValuesWhileAlsoSettingStaticAttributeToTrue() {
    return
      "db.taxonomyEntityWrapper.find({\"taxonomy.vocabularies.attributes.field\": {$regex: /\\.und$/}}).forEach(function (doc) {\n" +
      "  doc.taxonomy.vocabularies.filter(function (vocabulary) {\n" +
      "    return vocabulary.attributes.field && (vocabulary.attributes.field.match(/\\.und$/) || vocabulary.attributes.alias.match(/-und$/));\n" +
      "  }).forEach(function (undFieldVocabulary) {\n" +
      "    var fieldReplacement = undFieldVocabulary.attributes.field.replace(/(\\w+)\\.und$/, \"$1\");\n" +
      "    var aliasReplacement = undFieldVocabulary.attributes.alias.replace(/(\\w+)-und$/, \"$1\");\n" + "\n" +
      "    db.taxonomyEntityWrapper.update(\n" +
      "      {\"_id\": doc._id, \"taxonomy.vocabularies.name\": undFieldVocabulary.name},\n" +
      "      {$set: {\"taxonomy.vocabularies.$.attributes.field\": fieldReplacement, \"taxonomy.vocabularies.$.attributes.alias\": aliasReplacement, \"taxonomy.vocabularies.$.attributes.static\": \"true\", \"taxonomy.vocabularies.$.attributes.localized\": \"true\"}}\n" +
      "    );\n" +
      "  });\n" +
      "});";
  }

  private String updateDatasetClassNameVocabularyDescriptionAndTermTitleAndTermDescription() {
    return
      "var classNameVocabulary = db.taxonomyEntityWrapper.findOne({_id: \"dataset\"}).taxonomy.vocabularies.filter(function (vocabulary) {return vocabulary.name === \"className\";})[0];\n" +
      "\n" +
      "if (classNameVocabulary) {\n" +
      "  if (classNameVocabulary.description && classNameVocabulary.description.en) {\n" +
      "    classNameVocabulary.description.en = classNameVocabulary.description.en.replace(\"study\", \"collected\").replace(\"harmonization\", \"harmonized\");\n" +
      "  }\n" +
      "\n" +
      "  if (classNameVocabulary.terms && classNameVocabulary.terms.length > 0) {\n" +
      "    classNameVocabulary.terms.forEach(function (term) { \n" +
      "      if (term.title.en) {\n" +
      "        term.title.en = term.title.en.replace(\"Study\", \"Collected\").replace(\"Harmonization\", \"Harmonized\");\n" +
      "      }\n" +
      "\n" +
      "      if (term.title.fr) {\n" +
      "        term.title.fr = term.title.fr.replace(\"Étude\", \"Collecte\").replace(\"Harmonisation\", \"Harmonisée\");\n" +
      "      }\n" +
      "\n" +
      "      if (term.description.en) {\n" +
      "        term.description.en = term.description.en.replace(\"study dataset\", \"collected dataset\").replace(\"harmonization dataset\", \"harmonized dataset\");\n" +
      "      }\n" +
      "\n" +
      "      if (term.description.fr) {\n" +
      "        term.description.fr = term.description.fr.replace(\"une étude.\", \"une étude de collecte.\");\n" +
      "      }\n" +
      "    });\n" +
      "\n" +
      "    db.taxonomyEntityWrapper.update({_id: \"dataset\", \"taxonomy.vocabularies.name\": \"className\"}, {$set: {\"taxonomy.vocabularies.$\": classNameVocabulary}});\n" +
      "  }\n" +
      "}";
  }
}
