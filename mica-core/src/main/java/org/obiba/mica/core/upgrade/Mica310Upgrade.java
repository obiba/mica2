package org.obiba.mica.core.upgrade;

import com.google.common.collect.ImmutableList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.git.CommitInfo;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Component
public class Mica310Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private TaxonomyConfigService taxonomyConfigService;

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

    setupDatasetOrders();

    republishStudiesWithInvalidContent();

    addDefaultFacets();
  }

  private void addDefaultFacets() {

    logger.info("Add default facets in study taxonomy");
    ImmutableList<String> vocabulariesWithFacet = ImmutableList.<String>builder()
      .add("methods-design")
      .add("start")
      .add("end")
      .add("populations-selectionCriteria-countriesIso")
      .add("populations-selectionCriteria-ageMin")
      .add("populations-selectionCriteria-ageMax")
      .add("populations-selectionCriteria-gender")
      .add("populations-selectionCriteria-pregnantWomen")
      .add("populations-selectionCriteria-newborn")
      .add("populations-selectionCriteria-twins")
      .add("numberOfParticipants-participant-number")
      .add("numberOfParticipants-sample-number")
      .add("methods-recruitments")
      .add("populations-recruitment-dataSources")
      .add("populations-dataCollectionEvents-dataSources")
      .add("populations-dataCollectionEvents-bioSamples")
      .add("access", "19")
      .build();

    Taxonomy studyTaxonomy = taxonomyConfigService.findByTarget(TaxonomyTarget.STUDY);
    for (Vocabulary vocabulary : studyTaxonomy.getVocabularies()) {
      if(vocabulariesWithFacet.contains(vocabulary.getName())) {
        vocabulary.addAttribute("facet", "true");
        vocabulary.addAttribute("facetPosition", "0");
        vocabulary.addAttribute("facetExpanded", "false");
      }
    }

    taxonomyConfigService.update(TaxonomyTarget.STUDY, studyTaxonomy);
  }

  private void republishStudiesWithInvalidContent() {

    List<Study> publishedStudies = individualStudyService.findAllPublishedStudies();

    for (Study publishedStudy : publishedStudies) {

      publishedStudy = transformToValidStudy(publishedStudy);

      EntityState studyState = individualStudyService.getEntityState(publishedStudy.getId());
      if (studyState.getRevisionsAhead() == 0) {
        individualStudyService.save(publishedStudy);
        individualStudyService.publish(publishedStudy.getId(), true);
      } else {
        Study draftStudy = individualStudyService.findStudy(publishedStudy.getId());
        draftStudy = transformToValidStudy(draftStudy);
        individualStudyService.save(publishedStudy);
        individualStudyService.publish(publishedStudy.getId(), true);
        individualStudyService.save(draftStudy);
      }
    }

    List<String> publishedStudiesIds = publishedStudies.stream().map(AbstractGitPersistable::getId).collect(toList());
    individualStudyService.findAllDraftStudies().stream()
      .filter(unknownStateStudy -> !publishedStudiesIds.contains(unknownStateStudy.getId()))
      .filter(this::containsInvalidData)
      .map(this::transformToValidStudy)
      .forEach(individualStudyService::save);

    removeTaxonomyTaxonomyFromMongo();
  }

  void removeTaxonomyTaxonomyFromMongo() {
    try {
      logger.info("Remove Taxonomy of Taxonomies from DB since it is no longer persisted.");
      mongoTemplate.execute(db -> db.eval("db.taxonomyEntityWrapper.deleteOne({_id: 'taxonomy'});"));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to removeTaxonomyTaxonomyFromMongo().", e);
    }

  }

  private boolean containsInvalidData(Study study) {
    return containsInvalidMethodsDesign(study)
      || containsInvalidExistingStudies(study)
      || !containsValidMethods(study, "recruitments")
      || !containsValidMethods(study, "otherDesign")
      || !containsValidMethods(study, "followUpInfo")
      || !containsValidMethods(study, "otherRecruitment")
      || !containsValidMethods(study, "info");
  }

  private boolean containsInvalidMethodsDesign(Study study) {
    try {
      Map<String, Object> methods = getModelMethods(study);
      if (methods.containsKey("designs") && !methods.containsKey("design"))
        return true;
    } catch (RuntimeException ignore) {
    }
    return false;
  }

  private boolean containsValidMethods(Study study, String methodsAttribute) {
    try {
      return (getModelMethods(study)).get(methodsAttribute) != null;
    } catch (RuntimeException ignore) {
      return false;
    }
  }

  private boolean containsRecruitment(JSONObject json) {
    try {
      JSONArray jsonArray = json.getJSONObject("methods").getJSONArray("recruitments");
      return jsonArray.length() > 0;
    } catch (RuntimeException | JSONException ignore) {
      return false;
    }
  }

  private boolean containsInvalidExistingStudies(Study study) {
    if (study.getPopulations() == null)
      return false;

    for (Population population : study.getPopulations()) {
      try {
        if (((List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources")).contains("existing_studies"))
          return true;
      } catch (RuntimeException ignore) {
      }
    }
    return false;
  }

  private Study transformToValidStudy(Study study) {
    if (containsInvalidData(study)) {
      transformMethodsDesign(study);
      transformExistingStudies(study);
      addLostMethods(study);
    }
    return study;
  }

  private void transformExistingStudies(Study study) {
    try {
      for (Population population : study.getPopulations()) {
        List<String> dataSources = (List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources");
        dataSources.remove("existing_studies");
        dataSources.add("exist_studies");
      }
    } catch (RuntimeException ignore) {
    }
  }

  private void transformMethodsDesign(Study study) {
    try {
      if (containsInvalidMethodsDesign(study)) {
        Map<String, Object> methods = getModelMethods(study);
        String methodsDesign = ((List<String>) methods.get("designs")).get(0);
        methods.put("design", methodsDesign);
      }
    } catch (RuntimeException ignore) {
    }
  }

  private Map<String, Object> getModelMethods(Study study) {
    return (Map<String, Object>) study.getModel().get("methods");
  }

  private void setupDatasetOrders() {
    try {
      logger.info("Updating \"study\" populations and data collection events by adding a weight property based on current index.");
      mongoTemplate.execute(db -> db.eval(addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex.", e);
    }

    logger.info("Indexing all Collected Datasets");
    collectedDatasetService.indexAll();
  }

  private void addLostMethods(Study study) {

    Iterable<CommitInfo> commitInfos = individualStudyService.getCommitInfos(study);

    List<JSONObject> history = StreamSupport.stream(commitInfos.spliterator(), false)
      .map(commit -> individualStudyService.getFromCommitAsJson(study, commit.getCommitId()))
      .collect(toList());

    addRecruitmentsIfMissing(study, history);
    addMethodsIfMissing(study, history, "otherDesign");
    addMethodsIfMissing(study, history, "followUpInfo");
    addMethodsIfMissing(study, history, "otherRecruitment");
    addMethodsIfMissing(study, history, "info");
  }

  private void addRecruitmentsIfMissing(Study study, List<JSONObject> history) {
    try {
      if (!containsRecruitments(study)) {
        Optional<List<String>> optionalRecruitments = history.stream()
          .filter(this::containsRecruitment)
          .findFirst()
          .map(this::extractRecruitments);

        optionalRecruitments.ifPresent(recruitments -> (getModelMethods(study)).put("recruitments", recruitments));
      }
    } catch (RuntimeException ignore) {
    }
  }

  private void addMethodsIfMissing(Study study, List<JSONObject> history, String methodsAttribute) {

    try {
      if (!containsValidMethods(study, methodsAttribute)) {
        Optional<Map<String, String>> optionalMethodsAttributeValue = history.stream()
          .filter(studyHistoryAsJson -> this.containsOldMethods(studyHistoryAsJson, methodsAttribute))
          .filter(Objects::nonNull)
          .map(studyHistoryAsJson -> this.extractMethodsLocalizedString(studyHistoryAsJson, methodsAttribute))
          .findFirst();

        optionalMethodsAttributeValue.ifPresent(
          methodsAttributeValue -> (getModelMethods(study)).put(methodsAttribute, methodsAttributeValue));
      }
    } catch (RuntimeException ignore) {
    }
  }

  private boolean containsRecruitments(Study study) {
    return study.getModel() != null
      && study.getModel().containsKey("methods")
      && (getModelMethods(study)).containsKey("recruitments");
  }

  private List<String> extractRecruitments(JSONObject jsonStudy) {
    try {
      List<String> recruitments = new ArrayList<>();
      JSONArray jsonArray = jsonStudy.getJSONObject("methods").getJSONArray("recruitments");
      for (int i = 0; i < jsonArray.length(); i++) {
        recruitments.add(jsonArray.getString(i));
      }
      return recruitments;
    } catch (JSONException ignore) {
      return new ArrayList<>(0);
    }
  }

  private boolean containsOldMethods(JSONObject study, String methodsAttribute) {
    try {
      return extractMethodsLocalizedString(study, methodsAttribute) != null;
    } catch (RuntimeException ignore) {
    }
    return false;
  }

  private Map<String, String> extractMethodsLocalizedString(JSONObject jsonStudy, String methodsAttribute) {
    try {
      JSONObject methods = jsonStudy.getJSONObject("methods").getJSONObject(methodsAttribute);
      if (methods != null && !methods.toString().equals("null")) {
        HashMap<String, String> localizedFields = new HashMap<>();
        Iterator localizedFieldKeys = methods.keys();
        while (localizedFieldKeys.hasNext()) {
          String key = (String) localizedFieldKeys.next();
          localizedFields.put(key, methods.getString(key));
        }
        return localizedFields;
      }
    } catch (JSONException ignore) {
    }
    return null;
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
