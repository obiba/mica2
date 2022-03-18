package org.obiba.mica.web.controller.domain;

import org.obiba.mica.core.domain.LocalizedString;

import java.util.List;

public class HarmonizationAnnotations {

  private Annotation status;

  private Annotation statusDetail;

  private Annotation rule;

  private Annotation comment;

  public HarmonizationAnnotations(List<Annotation> harmoAnnotations) {
    for (Annotation annotation : harmoAnnotations) {
      if (annotation.getVocabularyName().equals("status"))
        status = annotation;
      else if (annotation.getVocabularyName().equals("status_detail"))
        statusDetail = annotation;
      else if (annotation.getVocabularyName().equals("comment"))
        comment = annotation;
      else if (annotation.getVocabularyName().equals("rule"))
        rule = annotation;
    }
  }

  public boolean hasStatus() {
    return status != null;
  }

  public LocalizedString getStatusTitle() {
    return status.getVocabularyTitle();
  }

  public LocalizedString getStatusDescription() {
    return status.getVocabularyDescription();
  }

  public String getStatusValue() {
    return status.getTermName();
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
    return status.getTermTitle();
  }

  public LocalizedString getStatusValueDescription() {
    return status.getTermDescription();
  }

  public boolean hasStatusDetail() {
    return statusDetail != null;
  }

  public LocalizedString getStatusDetailTitle() {
    return statusDetail.getVocabularyTitle();
  }

  public LocalizedString getStatusDetailDescription() {
    return statusDetail.getVocabularyDescription();
  }

  public String getStatusDetailValue() {
    return statusDetail.getTermName();
  }

  public LocalizedString getStatusDetailValueTitle() {
    return statusDetail.getTermTitle();
  }

  public LocalizedString getStatusDetailValueDescription() {
    return statusDetail.getTermDescription();
  }

  public boolean hasRule() {
    return rule != null;
  }

  public LocalizedString getRuleTitle() {
    return rule.getVocabularyTitle();
  }

  public LocalizedString getRuleDescription() {
    return rule.getVocabularyDescription();
  }

  public LocalizedString getRuleValue() {
    return rule.getTermValues();
  }

  public boolean hasComment() {
    return comment != null;
  }

  public LocalizedString getCommentTitle() {
    return comment.getVocabularyTitle();
  }

  public LocalizedString getCommentDescription() {
    return comment.getVocabularyDescription();
  }

  public LocalizedString getCommentValue() {
    return comment.getTermValues();
  }
}
