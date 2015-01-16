package org.obiba.mica.config;

import java.util.List;

import org.obiba.mica.micaConfig.AggregationInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(locations = "classpath:/mica-aggregations.yml")
public class AggregationsConfiguration {
  private List<AggregationInfo> study;

  private List<AggregationInfo> variable;

  public List<AggregationInfo> getStudy() {
    return study;
  }

  public void setStudy(List<AggregationInfo> study) {
    this.study = study;
  }

  public List<AggregationInfo> getVariable() {
    return variable;
  }

  public void setVariable(List<AggregationInfo> variable) {
    this.variable = variable;
  }
}
