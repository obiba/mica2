/*
 *
 *  * Copyright (c) 2016 OBiBa. All rights reserved.
 *  *
 *  * This program and the accompanying materials
 *  * are made available under the terms of the GNU Public License v3.0.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.obiba.mica.core.upgrade;

import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

@Component
public class Mica2Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica2Upgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private NetworkRepository networkRepository;
  @Inject
  private StudyRepository studyRepository;
  @Inject
  private StudyDatasetRepository studyDatasetRepository;
  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;
  @Inject
  private PersonRepository personRepository;

  @Inject
  private NetworkService networkService;
  @Inject
  private StudyService studyService;
  @Inject
  private StudyDatasetService studyDatasetService;
  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Override
  public String getDescription() {
    return "Migrate data from mica 1.5.x to mica 2.0.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("2.0.0");
  }

  @Override
  public void execute(Version currentVersion) {

    logger.info("migration from mica 1.x to mica 2.x : START");

    upgradeMembershipsOfStudiesAndNetworks();

    migrateNetworks();
    migrateStudies();
    migrateStudyDataset();
    migrateHarmonizationDataset();

    migrateContactCountries();

    reindexRequiredData();

    logger.info("migration from mica 1.x to mica 2.x : END");
  }

  private void reindexRequiredData() {

    logger.debug("Indexing all networks in the repository.");
    networkService.indexAll();

    logger.debug("Indexing all studies in the repository.");
    studyService.indexAll();

    logger.debug("Indexing all study datasets in the repository.");
    studyDatasetService.indexAll(false);

    logger.debug("Indexing all harmonization datasets in the repository.");
    harmonizationDatasetService.indexAll(false);
  }

  private void migrateContactCountries() {

    for (Person person : personRepository.findAllWhenCountryIsoContainsTwoCharacters()) {
      String countryIso2 = person.getInstitution().getAddress().getCountryIso();
      countryIso2 = cleanIso2Standart(countryIso2);
      String countryIso3 = new Locale("", countryIso2).getISO3Country();
      person.getInstitution().getAddress().setCountryIso(countryIso3);
      personRepository.save(person);
    }
  }

  private String cleanIso2Standart(String countryIso2) {
    //UK doesn't exists in ISO 2 standart
    return "UK".equals(countryIso2) ? "GB" : countryIso2;
  }

  private void migrateNetworks() {
    List<Network> networksWithoutModel = networkRepository.findWithoutModel();
    if (!networksWithoutModel.isEmpty()) {
      logger.info("Migrating networks from 1.x to 2.x: START");
      for (Network networkWithoutModel : networksWithoutModel) {
        networkWithoutModel.getModel();
        networkRepository.save(networkWithoutModel);
      }
      logger.info("Migrating networks: END");
    }
  }

  private void migrateStudies() {
    List<Study> studiesWithoutModel = studyRepository.findWithoutModel();
    if (!studiesWithoutModel.isEmpty()) {
      logger.info("Migrating studies 1.x to 2.x: START");
      for (Study studyWithoutModel : studiesWithoutModel) {
        studyWithoutModel.getModel();
        if (studyWithoutModel.getMethods() != null
          && studyWithoutModel.getMethods().getDesigns() != null
          && studyWithoutModel.getMethods().getDesigns().size() == 1)

          studyWithoutModel.getMethods().setDesign(studyWithoutModel.getMethods().getDesigns().get(0));

        studyWithoutModel.getPopulations().forEach(Population::getModel);
        studyWithoutModel.getPopulations().forEach(p -> p.getDataCollectionEvents().forEach(DataCollectionEvent::getModel));
        studyRepository.save(studyWithoutModel);
      }
      logger.info("Migrating studies: END");
    }
  }

  private void migrateStudyDataset() {
    List<StudyDataset> studyDatasetsWithoutModel = studyDatasetRepository.findWithoutModel();
    if (!studyDatasetsWithoutModel.isEmpty()) {
      logger.info("Migrating study datasets 1.x to 2.x: START");
      for (StudyDataset studyDatasetWithoutModel : studyDatasetsWithoutModel) {
        studyDatasetWithoutModel.getModel();
        studyDatasetRepository.save(studyDatasetWithoutModel);
      }
      logger.info("Migrating study datasets: END");
    }
  }

  private void migrateHarmonizationDataset() {
    List<HarmonizationDataset> harmonizationDatasetsWithoutModel = harmonizationDatasetRepository.findWithoutModel();
    if (!harmonizationDatasetsWithoutModel.isEmpty()) {
      logger.info("Migrating harmonization datasets 1.x to 2.x: START");
      for (HarmonizationDataset harmonizationDatasetWithoutModel : harmonizationDatasetsWithoutModel) {
        harmonizationDatasetWithoutModel.getModel();
        harmonizationDatasetRepository.save(harmonizationDatasetWithoutModel);
      }
      logger.info("Migrating harmonization datasets: END");
    }
  }

  private void upgradeMembershipsOfStudiesAndNetworks() {
    logger.info("start upgrade memberships of studies and networks ");
    mongoTemplate.execute(db -> db.eval(queryToUpgradeMembershipsOfStudies()));
    mongoTemplate.execute(db -> db.eval(queryToUpgradeMembershipsOfNetworks()));
    logger.info("end upgrade memberships of studies and networks ");
  }

  private String queryToUpgradeMembershipsOfStudies() {
    return upgradeMembershipsOfCollection("study");
  }

  private String queryToUpgradeMembershipsOfNetworks() {
    return upgradeMembershipsOfCollection("network");
  }

  private String upgradeMembershipsOfCollection(String collectionName) {

    return "db." + collectionName + ".find({memberships: {$exists: false}}).map(function (doc) {\n" +
      "\n" +
      "    memberships = {};\n" +
      "\n" +
      "    if (doc.contacts != undefined) {\n" +
      "        if (doc.contacts.length > 0) {\n" +
      "\n" +
      "            var contactIndex = 0;\n" +
      "            memberships.contact = [];\n" +
      "\n" +
      "            doc.contacts.forEach(function (person) {\n" +
      "                memberships.contact[contactIndex] = {\n" +
      "                    'role': 'contact',\n" +
      "                    'person': person\n" +
      "                };\n" +
      "                contactIndex++;\n" +
      "            });\n" +
      "        }\n" +
      "        delete doc.contacts;\n" +
      "    }\n" +
      "\n" +
      "    if (doc.investigators != undefined) {\n" +
      "        if (doc.investigators.length > 0) {\n" +
      "\n" +
      "            var investigatorsIndex = 0;\n" +
      "            memberships.investigator = [];\n" +
      "\n" +
      "            doc.investigators.forEach(function (person) {\n" +
      "                memberships.investigator[investigatorsIndex] = {\n" +
      "                    'role': 'investigator',\n" +
      "                    'person': person\n" +
      "                };\n" +
      "                investigatorsIndex++;\n" +
      "            });\n" +
      "        }\n" +
      "        delete doc.investigators;\n" +
      "    }\n" +
      "\n" +
      "    if (memberships.investigator !== undefined || memberships.contacts !== undefined)\n" +
      "        doc.memberships = memberships;\n" +
      "\n" +
      "    printjson(doc);\n" +
      "\n" +
      "    return doc;\n" +
      "}).forEach(function (doc) {\n" +
      "    db." + collectionName + ".update({'_id': doc._id}, doc);\n" +
      "});";
  }
}
