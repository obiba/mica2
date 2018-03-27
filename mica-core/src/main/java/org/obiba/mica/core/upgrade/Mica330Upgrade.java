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
public class Mica330Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private EventBus eventBus;

  private static final Logger logger = LoggerFactory.getLogger(Mica330Upgrade.class);

  @Override
  public String getDescription() {
    return "Upgrade data to 3.3.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 3, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.3.0");

    try {
      logger.info("Checking presence of \"sets\" vocabulary in current Mica_variable taxonomy");
      mongoTemplate.execute(db -> db.eval(addSetsVocabularyToVariableTaxonomyIfMissing()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addSetsVocabularyToVariableTaxonomyIfMissing.", e);
    }

    try {
      logger.info("Indexing Taxonomies");
      eventBus.post(new TaxonomiesUpdatedEvent());
    } catch(Exception e) {
      logger.error("Failed to index Taxonomies", e);
    }
  }

  private String addSetsVocabularyToVariableTaxonomyIfMissing() {
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
        "                \"terms\" : [ \n" +
        "                    {\n" +
        "                        \"name\" : \"cart\",\n" +
        "                        \"title\" : {\n" +
        "                            \"en\" : \"Cart\",\n" +
        "                            \"fr\" : \"Panier\"\n" +
        "                        },\n" +
        "                        \"description\" : {},\n" +
        "                        \"keywords\" : {},\n" +
        "                        \"attributes\" : {}\n" +
        "                    }\n" +
        "                ],\n" +
        "                \"attributes\" : {\n" +
        "                    \"static\" : \"true\",\n" +
        "                    \"hidden\" : \"true\"\n" +
        "                }\n" +
        "            };\n" +
        "\n" +
        "if (db.taxonomyEntityWrapper.find({\"_id\": \"variable\", \"taxonomy.vocabularies\": {$elemMatch : {\"name\": \"sets\"}}}).count() === 0) {\n" +
        "  db.taxonomyEntityWrapper.update({\"_id\": \"variable\"}, {$push: {\"taxonomy.vocabularies\": setsVocabulary}});\n" +
        "}\n";
  }

}
