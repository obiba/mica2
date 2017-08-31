package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class Mica310Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private CollectionDatasetService collectionDatasetService;

  private static final Logger logger = LoggerFactory.getLogger(Mica310Upgrade.class);

  @Override
  public String getDescription() {
    return "Migrate data to mica 3.1.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 1, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.1.0");

    try {
      logger.info("Updating \"study\" populations and data collection events by adding a weight property based on current index.");
      mongoTemplate.execute(db -> db.eval(addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex()));
    } catch(RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex.", e);
    }

    logger.info("Indexing all Collected Datasets");
    collectionDatasetService.indexAll();
  }

  private String addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex() {
    return
      "db.study.find({}).forEach(function (study) {\n" +
      "  study.populations.forEach(function (population, populationIndex) {\n" +
      "    population.weight = populationIndex;\n" +
      "    population.dataCollectionEvents.forEach(function (dataCollectionEvent, dceIndex) {\n" +
      "      dataCollectionEvent.weight = dceIndex;\n" +
      "    });\n" +
      "  });\n" +
      "\n" +
      "  db.study.update(\n" +
      "    {_id: study._id},\n" +
      "    {$set: study}\n" +
      "  );\n" +
      "});";
  }
}
