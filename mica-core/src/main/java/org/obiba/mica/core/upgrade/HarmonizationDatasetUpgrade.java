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

import java.util.List;

import javax.inject.Inject;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HarmonizationDatasetUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(HarmonizationDatasetUpgrade.class);

  @Inject
  private EventBus eventBus;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Override
  public String getDescription() {
    return "Added network tables to harmonization datasets.";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.5.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing harmonization datasets network tables upgrade");
    List<HarmonizationDataset> datasets = harmonizationDatasetRepository.findAll();

    datasets.forEach(dataset -> {
      int i = 0;
      for(StudyTable st: dataset.getStudyTables()) {
        st.setWeight(i++);
      }
    });

    harmonizationDatasetRepository.save(datasets);

    eventBus.post(new IndexDatasetsEvent());
  }
}
