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

import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SchemaFormUpgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(SchemaFormUpgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Override
  public String getDescription() {
    return "Default study schema form has changed. " +
      "So we need to update model of studies if the new schema form is used (so if the application upgraded from 1.x.x to 2.1.x)";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 1, 0);
  }

  @Override
  public boolean mustBeApplied(Version previousVersion, Version runtimeVersion) {
    return new Version(2, 0, 0).compareTo(previousVersion) > 0
      && new Version(2, 1, 0).compareTo(runtimeVersion) <= 0;
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("migration to 2.1.0 : START");
    mongoTemplate.execute(db -> db.eval(mongoQueryMoveSomePopulationCriterias()));
    logger.info("migration to 2.1.0 : END");
  }

  public String mongoQueryMoveSomePopulationCriterias() {
    return "db.study.find({'populations.model.selectionCriteria.criteria': {$exists: true}}).map(function (doc) {\n" +
      "\n" +
      "    doc.populations.forEach(function (population) {\n" +
      "        if (population.model != undefined\n" +
      "                && population.model.selectionCriteria != undefined\n" +
      "                && population.model.selectionCriteria.criteria != undefined\n" +
      "        ) {\n" +
      "\n" +
      "            if (population.model.selectionCriteria.criteria.indexOf('gravidity') >= 0) {\n" +
      "                population.model.selectionCriteria.pregnantWomen = ['first_trimester','second_trimester','third_trimester'];\n" +
      "            }\n" +
      "            if (population.model.selectionCriteria.criteria.indexOf('newborn') >= 0) {\n" +
      "                population.model.selectionCriteria.newborn = true;\n" +
      "            }\n" +
      "            if (population.model.selectionCriteria.criteria.indexOf('twin') >= 0) {\n" +
      "                population.model.selectionCriteria.twins = true;\n" +
      "            }\n" +
      "\n" +
      "            delete population.model.selectionCriteria.criteria;\n" +
      "            delete population.model.selectionCriteria._class;\n" +
      "        }\n" +
      "    });\n" +
      "\n" +
      "    return doc;\n" +
      "\n" +
      "}).forEach(function (doc) {\n" +
      "    db.study.update({'_id': doc._id}, doc);\n" +
      "});";
  }
}
