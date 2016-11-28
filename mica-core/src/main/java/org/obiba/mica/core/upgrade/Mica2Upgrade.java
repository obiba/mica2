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
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class Mica2Upgrade {

  private static final Logger logger = LoggerFactory.getLogger(Mica2Upgrade.class);

  @Autowired
  private NetworkRepository networkRepository;
  @Autowired
  private NetworkService networkService;

  @Autowired
  private StudyRepository studyRepository;
  @Autowired
  private StudyService studyService;

  @Autowired
  private StudyDatasetRepository studyDatasetRepository;
  @Autowired
  private StudyDatasetService studyDatasetService;

  @Autowired
  private HarmonizationDatasetRepository harmonizationDatasetRepository;
  @Autowired
  private HarmonizationDatasetService harmonizationDatasetService;

  @PostConstruct
  public void upgradeFromMica1_5ToMica2_0() {

    logger.info("migration from mica 1.x to mica 2.x : START");

    logger.info("migration networks : START");
    List<Network> networksWithoutModel = networkRepository.findWithoutModel();
    for (Network networkWithoutModel : networksWithoutModel) {
      networkWithoutModel.getModel();
      networkService.save(networkWithoutModel, "Upgrade from mica 1.x to mica 2.x");
    }
    logger.info("migration networks : END");

    logger.info("migration studies : START");
    List<Study> studiesWithoutModel = studyRepository.findWithoutModel();
    for (Study studyWithoutModel : studiesWithoutModel) {
      studyWithoutModel.getModel();
      studyService.save(studyWithoutModel, "Upgrade from mica 1.x to mica 2.x");
    }
    logger.info("migration studies : END");

    logger.info("migration studyDataset : START");
    List<StudyDataset> studyDatasetsWithoutModel = studyDatasetRepository.findWithoutModel();
    for (StudyDataset studyDatasetWithoutModel : studyDatasetsWithoutModel) {
      studyDatasetWithoutModel.getModel();
      studyDatasetService.save(studyDatasetWithoutModel, "Upgrade from mica 1.x to mica 2.x");
    }
    logger.info("migration studyDataset : END");

    logger.info("migration harmonizationDataset : START");
    List<HarmonizationDataset> harmonizationDatasetsWithoutModel = harmonizationDatasetRepository.findWithoutModel();
    for (HarmonizationDataset harmonizationDatasetWithoutModel : harmonizationDatasetsWithoutModel) {
      harmonizationDatasetWithoutModel.getModel();
      harmonizationDatasetService.save(harmonizationDatasetWithoutModel);
    }
    logger.info("migration harmonizationDataset : END");

    logger.info("migration from mica 1.x to mica 2.x : END");
  }
}
