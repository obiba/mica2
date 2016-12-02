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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Component
public class Mica2Upgrade {

  private static final Logger logger = LoggerFactory.getLogger(Mica2Upgrade.class);

  @Inject
  private NetworkRepository networkRepository;
  @Inject
  private NetworkService networkService;

  @Inject
  private StudyRepository studyRepository;
  @Inject
  private StudyService studyService;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;
  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;
  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @PostConstruct
  public void upgradeFromMica1_5ToMica2_0() {

    logger.info("migration from mica 1.x to mica 2.x : START");

    migrateNetworks();
    migrateStudies();
    migrateStudyDataset();
    migrateHarmonizationDataset();

    logger.info("migration from mica 1.x to mica 2.x : END");
  }

  private void migrateNetworks() {
    logger.info("migration networks : START");
    List<Network> networksWithoutModel = networkRepository.findWithoutModel();
    for (Network networkWithoutModel : networksWithoutModel) {
      networkWithoutModel.getModel();
      networkService.save(networkWithoutModel, "Upgrade from mica 1.x to mica 2.x");
    }
    logger.info("migration networks : END");
  }


  private void migrateStudies() {
    logger.info("migration studies : START");
    List<Study> studiesWithoutModel = studyRepository.findWithoutModel();
    for (Study studyWithoutModel : studiesWithoutModel) {
      studyWithoutModel.getModel();
      if (studyWithoutModel.getMethods().getDesigns() != null && studyWithoutModel.getMethods().getDesigns().size() == 1 && studyWithoutModel.getMethods().getDesign() == null)
        studyWithoutModel.getMethods().setDesign(studyWithoutModel.getMethods().getDesigns().get(0));
      studyWithoutModel.getPopulations().forEach(Population::getModel);
      studyWithoutModel.getPopulations().forEach(p -> p.getDataCollectionEvents().forEach(DataCollectionEvent::getModel));
      studyService.save(studyWithoutModel, "Upgrade from mica 1.x to mica 2.x");
    }
    logger.info("migration studies : END");
  }

  private void migrateStudyDataset() {
    logger.info("migration studyDataset : START");
    List<StudyDataset> studyDatasetsWithoutModel = studyDatasetRepository.findWithoutModel();
    for (StudyDataset studyDatasetWithoutModel : studyDatasetsWithoutModel) {
      studyDatasetWithoutModel.getModel();
      studyDatasetService.save(studyDatasetWithoutModel, "Upgrade from mica 1.x to mica 2.x");
    }
    logger.info("migration studyDataset : END");
  }

  private void migrateHarmonizationDataset() {
    logger.info("migration harmonizationDataset : START");
    List<HarmonizationDataset> harmonizationDatasetsWithoutModel = harmonizationDatasetRepository.findWithoutModel();
    for (HarmonizationDataset harmonizationDatasetWithoutModel : harmonizationDatasetsWithoutModel) {
      harmonizationDatasetWithoutModel.getModel();
      harmonizationDatasetService.save(harmonizationDatasetWithoutModel);
    }
    logger.info("migration harmonizationDataset : END");
  }
}
