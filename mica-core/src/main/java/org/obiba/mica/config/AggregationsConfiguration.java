package org.obiba.mica.config;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.obiba.mica.micaConfig.domain.AggregationInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

@ConfigurationProperties(locations = "classpath:/mica-aggregations.yml")
public class AggregationsConfiguration {

  private List<AggregationInfo> network;

  private List<AggregationInfo> study;

  private List<AggregationInfo> dataset;

  private List<AggregationInfo> variable;

  public void setNetwork(List<AggregationInfo> network) {
    this.network = network;
  }
  
  @NotNull
  public List<AggregationInfo> getNetwork() {
    return network == null ? network = Lists.newArrayList() : network;
  }

  public void setStudy(List<AggregationInfo> study) {
    this.study = study;
  }

  @NotNull
  public List<AggregationInfo> getStudy() {
    return study == null ? study = Lists.newArrayList() : study;
  }

  public void setDataset(List<AggregationInfo> dataset) {
    this.dataset = dataset;
  }

  @NotNull
  public List<AggregationInfo> getDataset() {
    return dataset == null ? dataset = Lists.newArrayList() : dataset;
  }

  public void setVariable(List<AggregationInfo> variable) {
    this.variable = variable;
  }

  @NotNull
  public List<AggregationInfo> getVariable() {
    return variable == null ? variable = Lists.newArrayList() : variable;
  }
}
