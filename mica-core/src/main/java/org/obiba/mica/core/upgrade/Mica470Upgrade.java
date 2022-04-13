package org.obiba.mica.core.upgrade;

import java.util.*;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;
import com.mongodb.client.model.DBCollectionUpdateOptions;

import javax.inject.Inject;

@Component
public class Mica470Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica470Upgrade.class);

  private final MongoTemplate mongoTemplate;

  private final EventBus eventBus;

  private final HarmonizationStudyService harmonizationStudyService;

  @Inject
  public Mica470Upgrade(MongoTemplate mongoTemplate, EventBus eventBus, HarmonizationStudyService harmonizationStudyService) {
    this.mongoTemplate = mongoTemplate;
    this.eventBus = eventBus;
    this.harmonizationStudyService = harmonizationStudyService;
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

    logger.info("Updating study taxonomies");
    updateStudyTaxonomies();

    try {
      logger.info("Updating 'Harmonization Initiative'...");
      updateHarmonizationInitiativeConfig();
    } catch (JSONException e) {
      logger.error("Error occurred while Updating 'Harmonization Initiative'");
    }

    try {
      logger.info("Updating 'Harmonization Protocol' form configuration");
      updateHarmonizationProtocolConfig();
    } catch (JSONException e) {
      logger.error("Error occurred while Updating 'Harmonization Protocol'");
    }
  }

  private DBObject getDBObjectSafely(String collectionName) {
    DBCursor dbCursor = mongoTemplate.execute(db -> db.getCollection(collectionName).find());
    if (dbCursor.hasNext()) {
      return dbCursor.next();
    }

    return null;
  }

  private void updateHarmonizationInitiativeConfig() throws JSONException {
    DBObject harmonizationStudyConfig = getDBObjectSafely("harmonizationStudyConfig");
    if (null != harmonizationStudyConfig) {
      JSONObject schema = new JSONObject(harmonizationStudyConfig.get("schema").toString());
      JSONArray definition = new JSONArray(harmonizationStudyConfig.get("definition").toString());

      ensureHarmonizationInitiativeNewFieldsExist(schema, definition);
      if (addPopulationsToHarmonizationInitiativeSchemaAndDefinition(schema, definition)) {
        copyPopulationConfigurationToStudyConfiguration(schema, definition);
        copyPopulationsToNewPopulationFields(schema, definition);
      }

      harmonizationStudyConfig.put("schema", schema.toString());
      harmonizationStudyConfig.put("definition", definition.toString());
      mongoTemplate.execute(db -> db.getCollection("harmonizationStudyConfig").save(harmonizationStudyConfig));
    }
  }

  private void copyPopulationConfigurationToStudyConfiguration(JSONObject schema, JSONArray definition) throws JSONException {
    DBObject harmonizationPopulationConfig = getDBObjectSafely("harmonizationPopulationConfig");
    if (null != harmonizationPopulationConfig) {
      JSONObject popSchema = new JSONObject(harmonizationPopulationConfig.get("schema").toString());

      if (popSchema.has("properties")) {
        JSONObject populationModelSchema = new JSONObject("{\"type\": \"object\"}");
        populationModelSchema.put("properties", popSchema.getJSONObject("properties"));
        schema.getJSONObject("properties").put("populationModel", populationModelSchema);
        definition.put("populationModel");
      }
    }
  }

  private void ensureHarmonizationInitiativeNewFieldsExist(JSONObject schema, JSONArray definition) throws JSONException {
    logger.info("Updating 'Harmonization Initiative' form configuration.");

    JSONObject properties = schema.getJSONObject("properties");

    if (properties.has("startYear")) {
      logger.info("'startYear' field already exists.");
    } else {
      logger.info("Adding 'Start Year' field");
      schema.getJSONObject("properties").put("startYear", new JSONObject("{\n" +
        "      \"type\": \"integer\",\n" +
        "      \"title\": \"t(study.start-year)\"\n" +
        "    }"));
      definition.put("startYear");
    }

    if (properties.has("endYear")) {
      logger.info("'endYear' field already exists.");
    } else {
      logger.info("Adding 'End Year' field");
      schema.getJSONObject("properties").put("endYear", new JSONObject("{\n" +
        "      \"type\": \"integer\",\n" +
        "      \"title\": \"t(study.end-year)\"\n" +
        "    }"));
      definition.put("endYear");
    }

    if (properties.has("additionalInformation")) {
      logger.info("'additionalInformation' field already exists.");
    } else {
      logger.info("Adding 'Additional Information' field");
      schema.getJSONObject("properties").put("additionalInformation", new JSONObject("{\n" +
        "      \"type\": \"object\",\n" +
        "      \"format\": \"obibaSimpleMde\",\n" +
        "      \"title\": \"t(global.additional-information)\"\n" +
        "    }"));
      definition.put("additionalInformation");
    }
  }

  private boolean addPopulationsToHarmonizationInitiativeSchemaAndDefinition(JSONObject schema, JSONArray definition) throws JSONException {
    logger.info("Adding Population field to 'Harmonization Initiative' to copy exising data.");

    JSONObject properties = schema.getJSONObject("properties");

    if (!properties.has("populations")) {
      JSONObject populationSchema = new JSONObject("{\n" +
        "      \"title\": \"t(study.populations)\",\n" +
        "      \"type\": \"array\",\n" +
        "      \"items\": {\n" +
        "        \"type\": \"object\",\n" +
        "        \"properties\": {\n" +
        "          \"id\": {\n" +
        "            \"title\": \"t(population.id)\",\n" +
        "            \"type\": \"string\"\n" +
        "          },\n" +
        "          \"name\": {\n" +
        "            \"title\": \"t(population.name)\",\n" +
        "            \"type\": \"object\",\n" +
        "            \"format\": \"localizedString\"\n" +
        "          },\n" +
        "          \"description\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"t(population.description)\",\n" +
        "            \"format\": \"obibaSimpleMde\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }");

      JSONObject populationDefinition = new JSONObject("{\n" +
        "    \"type\": \"section\",\n" +
        "    \"htmlClass\": \"row\",\n" +
        "    \"items\": [\n" +
        "      {\n" +
        "        \"type\": \"section\",\n" +
        "        \"htmlClass\": \"col-xs-6\",\n" +
        "        \"items\": [\n" +
        "          {\n" +
        "            \"key\": \"populations\",\n" +
        "            \"add\": \"t(study.add-population)\",\n" +
        "            \"startEmpty\": true,\n" +
        "            \"style\": {\n" +
        "              \"add\": \"btn-success\"\n" +
        "            },\n" +
        "            \"items\": [\n" +
        "              {\n" +
        "                \"key\": \"populations[].id\",\n" +
        "                \"required\": true\n" +
        "              },\n" +
        "              {\n" +
        "                \"key\": \"populations[].name\",\n" +
        "                \"type\": \"localizedstring\",\n" +
        "                \"required\": true\n" +
        "              },\n" +
        "              {\n" +
        "                \"key\": \"populations[].description\",\n" +
        "                \"type\": \"obibaSimpleMde\",\n" +
        "                \"required\": true\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        ]\n" +
        "      }\n" +
        "    ]\n" +
        "  }");

      // Add populations
      schema.getJSONObject("properties").put("populations", populationSchema);
      definition.put(populationDefinition);
      return true;
    } else {
      logger.warn("Schema already has a 'populations' field.");
    }

    return false;
  }

  private void copyPopulationsToNewPopulationFields(JSONObject schema, JSONArray definition) throws JSONException {
    logger.info("Coping population data to 'Harmonization Protocol'.");

    for (HarmonizationStudy study : harmonizationStudyService.findAllDraftStudies()) {
      Map<String, Object> studyModel = study.getModel();
      List<Map<String, Object>> populationsModel = new ArrayList<>();

      for (Population population : study.getPopulations()) {
        if (studyModel.containsKey("populations")) {
          logger.warn("Study {} already has 'populations', do not alter.", study.getId());
        } else {
          Map<String, Object> populationData = new HashMap<>();
          populationData.put("id", population.getId());
          populationData.put("name", new HashMap<>());
          populationData.put("description", new HashMap<>());
          population.getName().entrySet().stream().forEach(entry -> {
            ((Map<String, Object>) populationData.get("name")).put(entry.getKey(), entry.getValue());
          });
          population.getDescription().entrySet().stream().forEach(entry -> {
            ((Map<String, Object>) populationData.get("description")).put(entry.getKey(), entry.getValue());
          });
          populationsModel.add(populationData);

          // see if population has custom model
          if (population.hasModel()) {
            studyModel.put("populationModel", population.getModel());
          }
        }
      }

      studyModel.put("populations", populationsModel);
      study.setModel(studyModel);

      HarmonizationStudyState entityState = harmonizationStudyService.getEntityState(study.getId());
      boolean canPublish = entityState.isPublished() && !entityState.hasRevisionsAhead();
      harmonizationStudyService.save(study);

      if (canPublish) {
        harmonizationStudyService.publish(study.getId(), true);
      }
    }
  }

  private void updateHarmonizationProtocolConfig() throws JSONException {
    DBObject harmonizationDatasetConfig = getDBObjectSafely("harmonizationDatasetConfig");
    if (null != harmonizationDatasetConfig) {
      JSONObject schema = new JSONObject(harmonizationDatasetConfig.get("schema").toString());
      JSONArray definition = new JSONArray(harmonizationDatasetConfig.get("definition").toString());
      JSONObject properties = schema.getJSONObject("properties");

      // Ensure the new fields exists
      if (properties.has("version")) {
        logger.info("'Version' field already exists.");
      } else {
        logger.info("Adding 'Version' field");
        properties.put("version", new JSONObject("{\n" +
          "      \"type\": \"string\",\n" +
          "      \"title\": \"t(harmonization-protocol.version)\"\n" +
          "    }"));
        definition.put("version");
      }

      if (properties.has("participants")) {
        logger.info("'participants' field already exists.");
      } else {
        logger.info("Adding 'participants' field");
        properties.put("participants", new JSONObject("{\n" +
          "      \"type\": \"integer\",\n" +
          "      \"title\": \"t(harmonization-protocol.participants)\"\n" +
          "    }"));
        definition.put("participants");
      }

      if (properties.has("qualitativeQuantitative")) {
        logger.info("'qualitativeQuantitative' field already exists.");
      } else {
        logger.info("Adding 'qualitativeQuantitative' field");
        properties.put("qualitativeQuantitative", new JSONObject("{\n" +
          "      \"type\": \"string\",\n" +
          "      \"title\": \"t(harmonization-protocol.qualitative-quantitative.title)\",\n" +
          "      \"description\": \"t(harmonization-protocol.qualitative-quantitative.help)\",\n" +
          "      \"enum\": [\n" +
          "        \"qualitative\",\n" +
          "        \"quantitative\"\n" +
          "      ]\n" +
          "    }"));
        definition.put(new JSONObject("{\n" +
          "      \"key\": \"qualitativeQuantitative\",\n" +
          "      \"type\": \"radios\",\n" +
          "      \"titleMap\": [\n" +
          "        {\n" +
          "          \"value\": \"qualitative\",\n" +
          "          \"name\": \"t(harmonization-protocol.qualitative-quantitative.enum.qualitative)\"\n" +
          "        },\n" +
          "        {\n" +
          "          \"value\": \"quantitative\",\n" +
          "          \"name\": \"t(harmonization-protocol.qualitative-quantitative.enum.quantitative)\"\n" +
          "        }\n" +
          "      ]\n" +
          "    }"));
      }

      if (properties.has("prospectiveRetrospective")) {
        logger.info("'prospectiveRetrospective' field already exists.");
      } else {
        logger.info("Adding 'prospectiveRetrospective' field");
        properties.put("prospectiveRetrospective", new JSONObject("{\n" +
          "      \"type\": \"string\",\n" +
          "      \"title\": \"t(harmonization-protocol.prospective-retrospective.title)\",\n" +
          "      \"description\": \"t(harmonization-protocol.prospective-retrospective.help)\",\n" +
          "      \"enum\": [\n" +
          "        \"prospective\",\n" +
          "        \"retrospective\"\n" +
          "      ]\n" +
          "    }"));
        definition.put(new JSONObject("{\n" +
          "        \"key\": \"prospectiveRetrospective\",\n" +
          "        \"type\": \"radios\",\n" +
          "        \"titleMap\": [\n" +
          "          {\n" +
          "            \"value\": \"prospective\",\n" +
          "            \"name\": \"t(harmonization-protocol.prospective-retrospective.enum.prospective)\"\n" +
          "          },\n" +
          "          {\n" +
          "            \"value\": \"retrospective\",\n" +
          "            \"name\": \"t(harmonization-protocol.prospective-retrospective.enum.retrospective)\"\n" +
          "          }\n" +
          "        ]\n" +
          "      }"));
      }

      if (properties.has("informationContent")) {
        logger.info("'informationContent' field already exists.");
      } else {
        logger.info("Adding 'informationContent' field");
        properties.put("informationContent", new JSONObject("{\n" +
          "      \"type\": \"object\",\n" +
          "      \"title\": \"t(harmonization-protocol.information-content)\",\n" +
          "      \"format\": \"obibaSimpleMde\"\n" +
          "    }"));
        definition.put("informationContent");
      }

      if (properties.has("procedures")) {
        logger.info("'procedures' field already exists.");
      } else {
        logger.info("Adding 'procedures' field");
        properties.put("procedures", new JSONObject("{\n" +
          "      \"type\": \"object\",\n" +
          "      \"title\": \"t(harmonization-protocol.procedures)\",\n" +
          "      \"description\": \"t(harmonization-protocol.procedures-help)\",\n" +
          "      \"format\": \"localizedString\"\n" +
          "    }"));
        definition.put("procedures");
      }

      if (properties.has("participantsInclusion")) {
        logger.info("'participantsInclusion' field already exists.");
      } else {
        logger.info("Adding 'participantsInclusion' field");
        properties.put("participantsInclusion", new JSONObject("{\n" +
          "      \"type\": \"object\",\n" +
          "      \"title\": \"t(harmonization-protocol.participants-inclusion)\",\n" +
          "      \"format\": \"localizedString\"\n" +
          "    }"));
        definition.put("participantsInclusion");
      }

      if (properties.has("infrastructure")) {
        logger.info("'infrastructure' field already exists.");
      } else {
        logger.info("Adding 'infrastructure' field");
        properties.put("infrastructure", new JSONObject("{\n" +
          "      \"type\": \"object\",\n" +
          "      \"title\": \"t(harmonization-protocol.infrastructure)\",\n" +
          "      \"description\": \"t(harmonization-protocol.infrastructure-help)\",\n" +
          "      \"format\": \"localizedString\"\n" +
          "    }"));
        definition.put("infrastructure");
      }

      if (properties.has("additionalInformation")) {
        logger.info("'additionalInformation' field already exists.");
      } else {
        logger.info("Adding 'additionalInformation' field");
        properties.put("additionalInformation", new JSONObject("{\n" +
          "      \"type\": \"object\",\n" +
          "      \"title\": \"t(global.additional-information)\",\n" +
          "      \"format\": \"obibaSimpleMde\"\n" +
          "    }"));
        definition.put("additionalInformation");
      }

      harmonizationDatasetConfig.put("schema", schema.toString());
      harmonizationDatasetConfig.put("definition", definition.toString());
      mongoTemplate.execute(db -> db.getCollection("harmonizationDatasetConfig").save(harmonizationDatasetConfig));
    }
  }

  private void updateStudyTaxonomies() {
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

      DBCollectionUpdateOptions dbCollectionUpdateOptions = new DBCollectionUpdateOptions();
      dbCollectionUpdateOptions.multi(true).arrayFilters(Arrays.asList(arrayFilter));

      WriteResult updateResult = mongoTemplate.getCollection("taxonomyEntityWrapper").update(dbObject,
        BasicDBObject.parse("{ $set: { \"taxonomy.vocabularies.$[elem].attributes.forClassName\": \"Study\" } }"),
        dbCollectionUpdateOptions);

      if (updateResult.isUpdateOfExisting()) {
        logger.info("Added forClassName attribute for exclusive Study vocabularies");

        eventBus.post(new TaxonomiesUpdatedEvent());
      }
    }
  }
}
