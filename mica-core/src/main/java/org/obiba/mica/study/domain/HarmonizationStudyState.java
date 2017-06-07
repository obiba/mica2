package org.obiba.mica.study.domain;

import org.obiba.mica.core.domain.EntityState;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class HarmonizationStudyState extends EntityState {

  private static final long serialVersionUID = -3006707896847350709L;

  @Override
  public String pathPrefix() {
    return "harmonizationStudies";
  }
}
