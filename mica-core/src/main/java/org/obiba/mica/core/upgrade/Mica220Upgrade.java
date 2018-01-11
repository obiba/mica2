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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.obiba.mica.core.domain.TaxonomyEntityWrapper;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.repository.StudyConfigRepository;
import org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class Mica220Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica220Upgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private StudyConfigRepository studyConfigRepository;

  @Inject
  TaxonomyConfigRepository taxonomyConfigRepository;

  @Inject
  TaxonomyConfigService taxonomyConfigService;

  @Override
  public String getDescription() {
    return "Migrate data to mica 2.2.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 2, 0);
  }

  @Override
  public void execute(Version currentVersion) {

    try {
      updateBioSamplesAttributes();
    } catch (IOException e) {
      logger.info("Don't need migration to mica 2.2.0", e);
    } catch (Exception e) {
      logger.error("Error when trying to updateBioSamplesAttributes.", e);
    }

    try {
      updateStudyTaxonomyMissingFields();
    } catch (Exception e) {
      logger.error("Error when trying to updateStudyTaxonomyMissingFields.", e);
    }

    try {
      updateFixWrongKeysInStudyTaxonomy();
    } catch (Exception e) {
      logger.error("Error when trying to updateFixWrongKeysInStudyTaxonomy.", e);
    }

    try {
      updateTaxonomiesWithRangeCriteria();
    } catch (Exception e) {
      logger.error("Error when trying to updateTaxonomiesWithRangeCriteria.", e);
    }

    try {
      mergeDefaultTaxonomyWithCurrent();
    } catch (Exception e) {
      logger.error("Error when trying to mergeDefaultTaxonomyWithCurrent.", e);
    }

    try {
      removeUselessClassAttributeInModel();
    } catch (Exception e) {
      logger.error("Error when trying to removeUselessClassAttributeInModel.", e);
    }
  }

  private void removeUselessClassAttributeInModel() {
    mongoTemplate.execute(db -> db.eval("db.study.updateMany({\"model.methods._class\":{$exists:true}}, {$unset:{\"model.methods._class\":\"\"}})"));
  }

  private void updateTaxonomiesWithRangeCriteria() {
    Stream.of("network", "study", "dataset", "variable", "taxonomy")
      .forEach(name -> updateTaxonomyWithRangeCriteria(name));
  }

  private void mergeDefaultTaxonomyWithCurrent() {
    Stream.of("network", "study", "dataset", "variable", "taxonomy")
      .forEach(name -> taxonomyConfigService.mergeWithDefault(TaxonomyTarget.fromId(name)));
  }

  void updateTaxonomyWithRangeCriteria(String name) {
    TaxonomyEntityWrapper taxonomy = taxonomyConfigRepository.findOne(name);
    if (taxonomy == null) return;

    taxonomy.getTaxonomy().getVocabularies().stream()
      .filter(v -> v.getName().endsWith("-range"))
      .map(TaxonomyEntity::getAttributes)
      .forEach(attributes -> {
        attributes.put("alias", attributes.get("field").replaceAll("\\.", "-") + "-range");
        attributes.put("range", "true");
      });

    taxonomyConfigRepository.save(taxonomy);
  }

  private void updateFixWrongKeysInStudyTaxonomy() {

    TaxonomyEntityWrapper studyTaxonomy = taxonomyConfigRepository.findOne("study");
    if (studyTaxonomy == null) return;

    List<Vocabulary> vocabularies = studyTaxonomy.getTaxonomy().getVocabularies();

    vocabularies.stream()
      .filter(vocabulary -> vocabulary.getName().equals("numberOfParticipants-sample-number") || vocabulary.getName().equals("numberOfParticipants-sample-range"))
      .map(TaxonomyEntity::getAttributes)
      .forEach(attributes -> {
        attributes.put("field", "model." + attributes.get("field"));
        attributes.put("alias", "model-" + attributes.get("alias"));
      });

    taxonomyConfigRepository.save(studyTaxonomy);
  }

  private void updateStudyTaxonomyMissingFields() {
    mongoTemplate.execute(db -> db.eval(addMissingAttributesInStudyTaxonomy()));
  }

  private void updateBioSamplesAttributes() throws IOException {

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

  private String addBioSamplesAttributeIfNeededQuery() {
    return "db.study.updateMany({\"model.access\":\"biosamples\"}, {$push:{\"model.access\":\"bio_samples\"}})";
  }

  private String removeOutdatedBioSamplesAttributeQuery() {
    return "db.study.updateMany({\"model.access\":\"biosamples\"}, {$pull:{\"model.access\":\"biosamples\"}})";
  }

  private String addMissingAttributesInStudyTaxonomy() {
    return "db.taxonomyEntityWrapper.find({" +
      "    \"_id\": \"study\"," +
      "    \"taxonomy.vocabularies.name\": \"access\"," +
      "    \"taxonomy.vocabularies.name\": \"start\"," +
      "    \"taxonomy.vocabularies.name\": \"end\"" +
      "}).forEach(function (studyTaxo) {" +
      "" +
      "    studyTaxo.taxonomy.vocabularies.forEach(function (vocabulary) {" +
      "" +
      "        if (vocabulary.attributes.field == undefined) {" +
      "" +
      "            if (vocabulary.name == 'access') {" +
      "                vocabulary.attributes.field = \"model.access\";" +
      "                vocabulary.attributes.alias = \"model-access\";" +
      "            }" +
      "" +
      "            if (vocabulary.name == 'start') {" +
      "                vocabulary.attributes.field = \"model.startYear\";" +
      "                vocabulary.attributes.alias = \"model-startYear\";" +
      "            }" +
      "" +
      "            if (vocabulary.name == 'end') {" +
      "                vocabulary.attributes.field = \"model.endYear\";" +
      "                vocabulary.attributes.alias = \"model-endYear\";" +
      "            }" +
      "        }" +
      "    });" +
      "" +
      "    db.taxonomyEntityWrapper.update({'_id': studyTaxo._id}, studyTaxo);" +
      "});";
  }
}
