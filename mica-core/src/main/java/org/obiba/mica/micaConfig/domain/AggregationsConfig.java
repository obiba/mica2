package org.obiba.mica.micaConfig.domain;

import java.util.List;

import com.google.common.collect.Lists;

public class AggregationsConfig {
  private List<AggregationInfo> studyAggregations = Lists.newArrayList();

  private List<AggregationInfo> variableAggregations = Lists.newArrayList();

  public List<AggregationInfo> getStudyAggregations() {
    return studyAggregations;
  }

  public List<AggregationInfo> getVariableAggregations() {
    return variableAggregations;
  }

  public void setStudyAggregations(List<AggregationInfo> studyAggregations) {
    this.studyAggregations = studyAggregations;
  }

  public void setVariableAggregations(List<AggregationInfo> variableAggregations) {
    this.variableAggregations = variableAggregations;
  }
}
