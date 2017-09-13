package org.obiba.mica.core.upgrade;

import org.obiba.git.CommitInfo;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Component
public class Mica223RecoverStudiesDataUpgrade implements UpgradeStep {

  @Inject
  private StudyService studyService;

  private static final Logger logger = LoggerFactory.getLogger(Mica223RecoverStudiesDataUpgrade.class);

  @Override
  public String getDescription() {
    return "Migrate data to mica 2.2.3";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 2, 3);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 2.2.3");

    republishStudiesWithInvalidContent();
  }

  private void republishStudiesWithInvalidContent() {

    List<Study> publishedStudies = studyService.findAllPublishedStudies();

    for (Study publishedStudy : publishedStudies) {

      if (!containsInvalidData(publishedStudy))
        continue;

      publishedStudy = transformToValidStudy(publishedStudy);

      EntityState studyState = studyService.getEntityState(publishedStudy.getId());
      if (studyState.getRevisionsAhead() == 0) {
        studyService.save(publishedStudy);
        studyService.publish(publishedStudy.getId(), true);
      } else {
        Study draftStudy = studyService.findStudy(publishedStudy.getId());
        draftStudy = transformToValidStudy(draftStudy);
        studyService.save(publishedStudy);
        studyService.publish(publishedStudy.getId(), true);
        studyService.save(draftStudy);
      }
    }

    List<String> publishedStudiesIds = publishedStudies.stream().map(AbstractGitPersistable::getId).collect(toList());
    studyService.findAllDraftStudies().stream()
      .filter(unknownStateStudy -> !publishedStudiesIds.contains(unknownStateStudy.getId()))
      .filter(this::containsInvalidData)
      .map(this::transformToValidStudy)
      .forEach(studyService::save);
  }

  private boolean containsInvalidData(Study study) {
    return !study.hasModel()
      || containsInvalidMethodsDesign(study)
      || containsInvalidExistingStudies(study)
      || !containsMethods(study, "recruitments")
      || !containsMethods(study, "otherDesign")
      || !containsMethods(study, "followUpInfo")
      || !containsMethods(study, "otherRecruitment")
      || !containsMethods(study, "info");
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

  private boolean containsMethods(Study study, String methodsAttribute) {
    try {
      return (getModelMethods(study)).get(methodsAttribute) != null;
    } catch (RuntimeException ignore) {
      return false;
    }
  }

  private boolean containsRecruitment(Study study) {
    try {
      return study.getMethods() != null && study.getMethods().getRecruitments() != null && study.getMethods().getRecruitments().size() > 0;
    } catch (RuntimeException ignore) {
      return false;
    }
  }

  private boolean containsInvalidExistingStudies(Study study) {
    if (study.getPopulations() == null)
      return false;

    for (Population population : study.getPopulations()) {
      try {
        if (((List<String>) ((Map<String, Object>) population.getModel().get("recruitments")).get("dataSources")).contains("existing_studies"))
          return true;
      } catch (RuntimeException ignore) {
      }
    }
    return false;
  }

  private Study transformToValidStudy(Study study) {
    if (!study.hasModel())
      return study;
    transformMethodsDesign(study);
    transformExistingStudies(study);
    addLostMethods(study);
    return study;
  }

  private void transformExistingStudies(Study study) {
    try {
      for (Population population : study.getPopulations()) {
        List<String> dataSources = (List<String>) ((Map<String, Object>) population.getModel().get("recruitments")).get("dataSources");
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
    if (study.getModel().get("methods") instanceof Study.StudyMethods) {
      Study.StudyMethods methods = (Study.StudyMethods) study.getModel().get("methods");

      HashMap<String, Object> methodsAsMap = new HashMap<>();
      methodsAsMap.put("design", methods.getDesign());
      methodsAsMap.put("recruitments", methods.getRecruitments());
      methodsAsMap.put("otherDesign", methods.getOtherDesign());
      methodsAsMap.put("followUpInfo", methods.getFollowUpInfo());
      methodsAsMap.put("otherRecruitment", methods.getOtherRecruitment());
      methodsAsMap.put("info", methods.getInfo());

      return methodsAsMap;
    } else {
      return (Map<String, Object>) study.getModel().get("methods");
    }
  }

  private void addLostMethods(Study study) {

    Iterable<CommitInfo> commitInfos = studyService.getCommitInfos(study);

    List<Study> history = StreamSupport.stream(commitInfos.spliterator(), false)
      .map(commit -> studyService.getFromCommit(study, commit.getCommitId()))
      .collect(toList());

    addRecruitmentsIfMissing(study, history);
    addMethodsIfMissing(study, history, "otherDesign");
    addMethodsIfMissing(study, history, "followUpInfo");
    addMethodsIfMissing(study, history, "otherRecruitment");
    addMethodsIfMissing(study, history, "info");
  }

  private void addRecruitmentsIfMissing(Study study, List<Study> history) {
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

  private void addMethodsIfMissing(Study study, List<Study> history, String methodsAttribute) {

    try {
      if (!containsMethods(study, methodsAttribute)) {
        Optional<Object> optionalMethodsAttributeValue = history.stream()
          .map(t -> this.extractMethodsLocalizedString(t, methodsAttribute))
          .filter(Objects::nonNull)
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

  private List<String> extractRecruitments(Study study) {
    try {
      return study.getMethods().getRecruitments();
    } catch (RuntimeException ignore) {
      return new ArrayList<>(0);
    }
  }

  private Object extractMethodsLocalizedString(Study study, String methodsAttribute) {

    if((getModelMethods(study)).get(methodsAttribute) != null)
      return (getModelMethods(study)).get(methodsAttribute);

    try {
      switch (methodsAttribute) {
        case "otherDesign":
          return study.getMethods().getOtherDesign();
        case "followUpInfo":
          return study.getMethods().getFollowUpInfo();
        case "otherRecruitment":
          return study.getMethods().getOtherRecruitment();
        case "info":
          return study.getMethods().getInfo();
      }
    } catch (RuntimeException ignore) {
    }
    return null;
  }
}
