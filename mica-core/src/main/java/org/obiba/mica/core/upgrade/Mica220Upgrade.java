package org.obiba.mica.core.upgrade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.repository.StudyConfigRepository;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@Component
public class Mica220Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica220Upgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private StudyConfigRepository studyConfigRepository;

  @Override
  public String getDescription() {
    return "Migrate data to mica 2.2.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 4, 0);
  }

  @Override
  public void execute(Version currentVersion) {

    try {
      execute();
    } catch (IOException e) {
      logger.info("Don't need migration to mica 2.2.0", e);
    }

  }

  private void execute() throws IOException {

    List<StudyConfig> studyConfigs = studyConfigRepository.findAll();
    if (studyConfigs.size() != 1)
      return;

    StudyConfig studyConfig = studyConfigs.get(0);

    JsonNode schemaNode = new ObjectMapper().readTree(studyConfig.getSchema());
    if (!propertyExistsInSchema(schemaNode))
      return;

    JsonNode definitionNode = new ObjectMapper().readTree(studyConfig.getDefinition());
    if (!replacePropertyInDefinition(definitionNode))
      return;

    replacePropertyInSchema(schemaNode);

    studyConfig.setSchema(new ObjectMapper().writer().writeValueAsString(schemaNode));
    studyConfig.setDefinition(new ObjectMapper().writer().writeValueAsString(definitionNode));

    studyConfigRepository.save(studyConfig);

    updateAttributeNameInStudies();
  }

  private void updateAttributeNameInStudies() {
    mongoTemplate.execute(db -> db.eval(addBioSamplesAttributeIfNeededQuery()));
    mongoTemplate.execute(db -> db.eval(removeOutdatedBioSamplesAttributeQuery()));
  }

  private String addBioSamplesAttributeIfNeededQuery() {
    return "db.study.updateMany({\"model.access\":\"biosamples\"}, {$push:{\"model.access\":\"bio_samples\"}})";
  }

  private String removeOutdatedBioSamplesAttributeQuery() {
    return "db.study.updateMany({\"model.access\":\"biosamples\"}, {$pull:{\"model.access\":\"biosamples\"}})";
  }

  private void replacePropertyInSchema(JsonNode schemaNode) {
    JsonNode jsonNode = schemaNode.get("properties").get("access").get("items").get("enum");

    for (int i = 0; i < jsonNode.size(); i++) {
      if (jsonNode.get(i).textValue().equals("biosamples")) {
        ((ArrayNode) jsonNode).remove(i);
        ((ArrayNode) jsonNode).add("bio_samples");
        return;
      }
    }
  }

  private boolean replacePropertyInDefinition(JsonNode definitionNode) {
    Iterator<JsonNode> itemsLvl0 = definitionNode.elements();
    while (itemsLvl0.hasNext()) {
      JsonNode itemLvl0 = itemsLvl0.next();
      if (itemLvl0.has("items")) {
        Iterator<JsonNode> itemsLvl1 = itemLvl0.get("items").elements();
        while (itemsLvl1.hasNext()) {
          JsonNode itemLvl1 = itemsLvl1.next();
          if (itemLvl1.has("items")) {
            Iterator<JsonNode> itemsLvl2 = itemLvl1.get("items").elements();
            while (itemsLvl2.hasNext()) {
              JsonNode itemLvl2 = itemsLvl2.next();
              if (itemLvl2.has("titleMap")) {
                Iterator<JsonNode> titleMaps = itemLvl2.get("titleMap").elements();
                while (titleMaps.hasNext()) {
                  JsonNode titleMap = titleMaps.next();
                  if (titleMap.has("value") && titleMap.get("value").textValue().equals("biosamples")) {
                    ((ObjectNode) titleMap).put("value", "bio_samples");
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  private boolean propertyExistsInSchema(JsonNode schema) {

    Iterator<JsonNode> values = schema.get("properties").get("access").get("items").get("enum").elements();

    while (values.hasNext()) {
      JsonNode value = values.next();
      if (value.textValue().equals("biosamples"))
        return true;
    }

    return false;
  }
}
