package org.obiba.mica.web.controller.domain;

import org.obiba.mica.core.domain.LocalizedString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HarmonizationAnnotations {

  final private Map<String, Annotation> annotationMap = new HashMap<>();

  public HarmonizationAnnotations(List<Annotation> harmoAnnotations) {
    for (Annotation annotation : harmoAnnotations) {
      annotationMap.put(annotation.getVocabularyName(), annotation);
    }
  }

  public boolean hasStatus() {
    return hasAnnotation("status");
  }

  public LocalizedString getStatusTitle() {
    return getAnnotationTitle("status");
  }

  public LocalizedString getStatusDescription() {
    return getAnnotationDescription("status");
  }

  public String getStatusValue() {
    return getAnnotationValue("status");
  }

  public String getStatusClass() {
    if (!hasStatus()) return "info";

    switch (getStatusValue()) {
      case "complete":
        return "success";
      case "undetermined":
        return "warning";
      case "impossible":
        return "danger";
      default:
        return "info";
    }
  }

  public LocalizedString getStatusValueTitle() {
    return getAnnotationValueTitle("status");
  }

  public LocalizedString getStatusValueDescription() {
    return getAnnotationValueDescription("status");
  }

  public boolean hasStatusDetail() {
    return annotationMap.containsKey("status_detail");
  }

  public LocalizedString getStatusDetailTitle() {
    return getAnnotationTitle("status_detail");
  }

  public LocalizedString getStatusDetailDescription() {
    return getAnnotationDescription("status_detail");
  }

  public String getStatusDetailValue() {
    return getAnnotationValue("status_detail");
  }

  public LocalizedString getStatusDetailValueTitle() {
    return getAnnotationValueTitle("status_detail");
  }

  public LocalizedString getStatusDetailValueDescription() {
    return getAnnotationValueDescription("status_detail");
  }

  public boolean hasAlgorithm() {
    return annotationMap.containsKey("algorithm");
  }

  public LocalizedString getAlgorithmTitle() {
    return getAnnotationTitle("algorithm");
  }

  public LocalizedString getAlgorithmDescription() {
    return getAnnotationDescription("algorithm");
  }

  public LocalizedString getAlgorithmValue() {
    return getAnnotationValues("algorithm");
  }

  public boolean hasComment() {
    return annotationMap.containsKey("comment");
  }

  public LocalizedString getCommentTitle() {
    return getAnnotationTitle("comment");
  }

  public LocalizedString getCommentDescription() {
    return getAnnotationDescription("comment");
  }

  public LocalizedString getCommentValue() {
    return getAnnotationValues("comment");
  }

  public boolean hasAnnotation(String name) {
    return annotationMap.containsKey(name);
  }

  public LocalizedString getAnnotationTitle(String name) {
    return annotationMap.get(name).getVocabularyTitle();
  }

  public LocalizedString getAnnotationDescription(String name) {
    return annotationMap.get(name).getVocabularyDescription();
  }

  public String getAnnotationValue(String name) {
    return annotationMap.get(name).getTermName();
  }

  public LocalizedString getAnnotationValues(String name) {
    return annotationMap.get(name).getTermValues();
  }

  public LocalizedString getAnnotationValueTitle(String name) {
    return annotationMap.get(name).getTermTitle();
  }

  public LocalizedString getAnnotationValueDescription(String name) {
    return annotationMap.get(name).getTermDescription();
  }

}
