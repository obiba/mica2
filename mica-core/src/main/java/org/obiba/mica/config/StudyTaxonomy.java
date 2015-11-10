package org.obiba.mica.config;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

@ConfigurationProperties(locations = "classpath:/mica-studies.yml")
public class StudyTaxonomy extends Taxonomy {

  private static final long serialVersionUID = 4850803732637831829L;

  public StudyTaxonomy() {
    setVocabularies(Lists.newArrayList()); //explicit initialization for yaml bean factory
  }

}
