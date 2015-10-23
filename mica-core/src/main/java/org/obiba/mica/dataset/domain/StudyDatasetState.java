package org.obiba.mica.dataset.domain;

import org.obiba.mica.core.domain.EntityState;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class StudyDatasetState extends EntityState {
  @Override
  public String pathPrefix() {
    return "studyDatasets";
  }
}
