package org.obiba.mica.micaConfig.domain;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class AggregationsConfig {

  private List<AggregationInfo> networkAggregations = Lists.newArrayList();

  private List<AggregationInfo> studyAggregations = Lists.newArrayList();

  private List<AggregationInfo> datasetAggregations = Lists.newArrayList();

  private List<AggregationInfo> variableAggregations = Lists.newArrayList();

  public AggregationsConfig() {
  }

  public AggregationsConfig(AggregationsConfig source) {
    networkAggregations = Lists.newArrayList(source.networkAggregations);
    studyAggregations = Lists.newArrayList(source.studyAggregations);
    datasetAggregations = Lists.newArrayList(source.datasetAggregations);
    variableAggregations = Lists.newArrayList(source.variableAggregations);
  }

  public void setNetworkAggregations(List<AggregationInfo> networkAggregations) {
    this.networkAggregations = networkAggregations;
  }

  public List<AggregationInfo> getNetworkAggregations() {
    return ImmutableList.copyOf(networkAggregations);
  }

  public List<AggregationInfo> getStudyAggregations() {
    return ImmutableList.copyOf(studyAggregations);
  }

  public void setStudyAggregations(List<AggregationInfo> studyAggregations) {
    this.studyAggregations = studyAggregations;
  }

  public void setDatasetAggregations(List<AggregationInfo> datasetAggregations) {
    this.datasetAggregations = datasetAggregations;
  }

  public List<AggregationInfo> getDatasetAggregations() {
    return ImmutableList.copyOf(datasetAggregations);
  }

  public List<AggregationInfo> getVariableAggregations() {
    return ImmutableList.copyOf(variableAggregations);
  }

  public void setVariableAggregations(List<AggregationInfo> variableAggregations) {
    this.variableAggregations = variableAggregations;
  }

  public void addVariableAggregation(AggregationInfo aggregationInfo) {
    if(!containsVariableAggregation(aggregationInfo.getId())) {
      variableAggregations.add(aggregationInfo);
    }
  }

  private boolean containsVariableAggregation(String id) {
    for(AggregationInfo aggInfo : variableAggregations)
      if(aggInfo.getId().equals(id)) return true;
    return false;
  }
}
