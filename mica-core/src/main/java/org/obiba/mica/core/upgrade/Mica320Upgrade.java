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

import com.google.common.eventbus.EventBus;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class Mica320Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private EventBus eventBus;

  private static final Logger logger = LoggerFactory.getLogger(Mica320Upgrade.class);

  @Override
  public String getDescription() {
    return "Upgrade data to 3.2.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 2, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.2.0");

    try {
      logger.info("Updating variable's variableType vocabulary terms.");
      mongoTemplate.execute(db -> db.eval(variableTypeVocabularyTermsUpdate()));
    } catch(Exception e) {
      logger.error("Failed to update variable's variableType vocabulary terms", e);
    }

    try {
      logger.info("Indexing Taxonomies");
      eventBus.post(new TaxonomiesUpdatedEvent());
    } catch(Exception e) {
      logger.error("Failed to index Taxonomies", e);
    }

    try {
      logger.info("Re-indexing studies");
      eventBus.post(new IndexStudiesEvent());
    } catch(Exception e) {
      logger.error("Failed to re-index studies", e);
    }

    try {
      logger.info("Re-indexing networks");
      eventBus.post(new IndexNetworksEvent());
    } catch(Exception e) {
      logger.error("Failed to re-index networks", e);
    }
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
