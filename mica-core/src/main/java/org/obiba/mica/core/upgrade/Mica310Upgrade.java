package org.obiba.mica.core.upgrade;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.CollectionStudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Component
public class Mica310Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private CollectionDatasetService collectionDatasetService;

  @Inject
  private CollectionStudyService collectionStudyService;

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
    logger.info("Executing Mica upgrade to version 3.3.0");

//    setupDatasetOrders();

    republishStudiesWithExistingStudiesDataSources();
  }

  private void republishStudiesWithExistingStudiesDataSources() {

    List<Study> publishedStudies = collectionStudyService.findAllPublishedStudies();

    for (Study publishedStudy : publishedStudies) {

      if (!containsExistingStudies(publishedStudy))
        continue;

      publishedStudy = replaceExistingStudiesByExistStudies(publishedStudy);

      EntityState studyState = collectionStudyService.getEntityState(publishedStudy.getId());
      if (studyState.getRevisionsAhead() == 0) {
        collectionStudyService.save(publishedStudy);
        collectionStudyService.publish(publishedStudy.getId(), true);
      } else {
        Study draftStudy = collectionStudyService.findStudy(publishedStudy.getId());
        draftStudy = replaceExistingStudiesByExistStudies(draftStudy);
        collectionStudyService.save(publishedStudy);
        collectionStudyService.publish(publishedStudy.getId(), true);
        collectionStudyService.save(draftStudy);
      }
    }

    List<String> publishedStudiesIds = publishedStudies.stream().map(AbstractGitPersistable::getId).collect(toList());
    collectionStudyService.findAllDraftStudies().stream()
      .filter(unknownStateStudy -> !publishedStudiesIds.contains(unknownStateStudy.getId()))
      .filter(this::containsExistingStudies)
      .map(this::replaceExistingStudiesByExistStudies)
      .forEach(collectionStudyService::save);
  }

  private boolean containsExistingStudies(Study study) {
    try {
      for (Population population : study.getPopulations()) {
        if (((List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources")).contains("existing_studies"))
          return true;
      }
    } catch (RuntimeException ignore) {
    }
    return false;
  }

  private Study replaceExistingStudiesByExistStudies(Study study) {
    try {
      for (Population population : study.getPopulations()) {
        List<String> dataSources = (List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources");
        dataSources.remove("existing_studies");
        dataSources.add("exist_studies");
      }
    } catch (RuntimeException ignore) {
    }

    return study;
  }

  private void setupDatasetOrders() {
    try {
      logger.info("Updating \"study\" populations and data collection events by adding a weight property based on current index.");
      mongoTemplate.execute(db -> db.eval(addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex()));
    } catch (RuntimeException e) {
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
