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
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

@Component
public class DatasetStateUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(DatasetStateUpgrade.class);

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

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
    return new Version("0.9.2");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing datasets published state upgrade");

    studyDatasetRepository.findAll().forEach(dataset -> {
      migrateDataset(dataset, studyDatasetService, studyDatasetStateRepository, () -> {
        StudyDatasetState defaultState = new StudyDatasetState();
        return defaultState;
      });
    });

    harmonizationDatasetRepository.findAll().forEach(dataset -> {
      migrateDataset(dataset, harmonizationDatasetService, harmonizationDatasetStateRepository, () -> {
        HarmonizationDatasetState defaultState = new HarmonizationDatasetState();
        return defaultState;
      });
    });
  }

  private <T extends AbstractGitPersistableService, T1 extends EntityStateRepository> void migrateDataset(
    Dataset dataset, T datasetService, T1 stateRepository, Supplier<EntityState> supplier) {
    EntityState state = datasetService.findEntityState(dataset, supplier);

    state.incrementRevisionsAhead();
    stateRepository.save(state);
    eventBus.post(new DatasetUpdatedEvent(dataset, null));

    gitService.save(dataset, "System upgrade");

    if(dataset.isPublished()) {
      datasetService.publishState(dataset.getId());
      eventBus.post(new DatasetPublishedEvent(dataset, null, null));
    }
  }
}
