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

import java.util.function.Supplier;

import javax.inject.Inject;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
public class DatasetStateUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(DatasetStateUpgrade.class);

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private EventBus eventBus;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private GitService gitService;

  @Override
  public String getDescription() {
    return "Refactored datasets published state.";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.0.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing datasets published state upgrade");

    studyDatasetRepository.findAll().forEach(dataset -> {
      migrateDataset(dataset, collectedDatasetService, studyDatasetStateRepository, StudyDatasetState::new);
    });

    harmonizationDatasetRepository.findAll().forEach(dataset -> {
      migrateDataset(dataset, harmonizedDatasetService, harmonizationDatasetStateRepository, HarmonizationDatasetState::new);
    });
  }

  private <T extends AbstractGitPersistableService, T1 extends EntityStateRepository> void migrateDataset(
    Dataset dataset, T datasetService, T1 stateRepository, Supplier<EntityState> supplier) {
    EntityState state = datasetService.findEntityState(dataset, supplier);

    state.incrementRevisionsAhead();
    stateRepository.save(state);
    eventBus.post(new DatasetUpdatedEvent(dataset));

    gitService.save(dataset, "System upgrade");

    if(state.isPublished()) {
      datasetService.publishState(dataset.getId());
      eventBus.post(new DatasetPublishedEvent(dataset, null, null));
    }
  }
}
