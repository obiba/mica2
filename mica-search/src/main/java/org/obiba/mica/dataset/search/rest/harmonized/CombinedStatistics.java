package org.obiba.mica.dataset.search.rest.harmonized;

import java.lang.Math;

import org.obiba.mica.web.model.Mica;

public class CombinedStatistics {

  private Mica.StatisticsDto stats;

  private int count = 0;

  private Mica.StatisticsDto tableStats;

  private int tableCount = 0;

  CombinedStatistics(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    stats = Mica.StatisticsDto.newBuilder(aggDto.getStatistics()).build();
    tableStats = tableAggDto.getStatistics();
    count = aggDto.getN();
    tableCount = tableAggDto.getN();
  }

  public int getCount() {
    return count + tableCount;
  }

  public float getSum() {
    return (stats.hasSum() ? stats.getSum() : 0) + (tableStats.hasSum() ? tableStats.getSum() : 0);
  }

  public boolean hasMean() {
    return getCount() > 0;
  }

  public float getMean() {
    return hasMean() ? getSum() / getCount() : 0;
  }

  public boolean hasMin() {
    return tableStats.hasMin() && tableStats.getMin() != Float.POSITIVE_INFINITY;
  }

  public float getMin() {
    return hasMin() ? stats.hasMin() ? Math.min(stats.getMin(), tableStats.getMin()) : tableStats.getMin() : 0;
  }

  public boolean hasMax() {
    return tableStats.hasMax() && tableStats.getMax() != Float.NEGATIVE_INFINITY;
  }

  public float getMax() {
    return hasMax() ? stats.hasMax() ? Math.max(stats.getMax(), tableStats.getMax()) : tableStats.getMax() : 0;
  }

  public boolean hasSumOfSquares() {
    return tableStats.hasSumOfSquares();
  }

  public float getSumOfSquares() {
    return hasSumOfSquares()
      ? (stats.hasSumOfSquares() ? 0 : stats.getSumOfSquares()) + tableStats.getSumOfSquares()
      : 0;
  }

  public float getVariance() {
    float mean = getMean();

    // ESSG = error sum of squares within each group = variance * (n-1)
    // ESS = error sum of squares = sum(var(i) * (n(i)-1))
    float essg = (stats.hasVariance() ? stats.getVariance() : 0) * (count - 1);
    float tableEssg = (tableStats.hasVariance() ? tableStats.getVariance() : 0) * (tableCount - 1);
    float ess = essg + tableEssg;

    // GM = grand mean = sum(n(i) * mean(i))
    float tableMean = tableStats.hasMean() ? tableStats.getMean() : 0;
    float gm = (mean * count + tableMean * tableCount) / getCount();

    // GSS = group sum of squares = (mean(i) - gm)^2 * n(i)
    float gss = Double.valueOf(Math.pow(mean - gm, 2)).floatValue() * count;
    float tableGss = Double.valueOf(Math.pow(tableMean - gm, 2)).floatValue() * tableCount;
    float tgss = gss + tableGss;

    // GV = grand variance
    return getCount() == 1 ? 0 : (ess - tgss) / (getCount() - 1);
  }

  //
  // Static methods
  //

  public static void mergeAggregations(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    mergeFrequencies(aggDto, tableAggDto);
    mergeStatistics(aggDto, tableAggDto);
    aggDto.setTotal(aggDto.getTotal() + tableAggDto.getTotal());
    aggDto.setN(aggDto.getN() + tableAggDto.getN());
  }

  public static void mergeFrequencies(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    if(tableAggDto.getFrequenciesCount() == 0) return;

    for(Mica.FrequencyDto tableFreq : tableAggDto.getFrequenciesList()) {
      boolean found = false;
      for(int i = 0; i < aggDto.getFrequenciesCount(); i++) {
        Mica.FrequencyDto freq = aggDto.getFrequencies(i);
        if(freq.getValue().equals(tableFreq.getValue())) {
          aggDto.setFrequencies(i, freq.toBuilder().setCount(freq.getCount() + tableFreq.getCount()).build());
          found = true;
          break;
        }
      }
      if(!found) {
        aggDto.addFrequencies(tableFreq.toBuilder());
      }
    }
  }

  public static void mergeStatistics(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    if(!tableAggDto.hasStatistics()) return;

    if(!aggDto.hasStatistics()) {
      aggDto.setStatistics(tableAggDto.getStatistics().toBuilder());
    } else {
      CombinedStatistics combined = new CombinedStatistics(aggDto, tableAggDto);
      if(combined.getCount() > 0) {
        Mica.StatisticsDto.Builder builder = aggDto.getStatistics().toBuilder();

        builder.setSum(combined.getSum());
        if(combined.hasMean()) builder.setMean(combined.getMean());
        if(combined.hasMin()) builder.setMin(combined.getMin());
        if(combined.hasMax()) builder.setMax(combined.getMax());
        if(combined.hasSumOfSquares()) builder.setSumOfSquares(combined.getSumOfSquares());

        float gv = combined.getVariance();
        builder.setVariance(gv);
        builder.setStdDeviation(Double.valueOf(Math.pow(gv, 0.5)).floatValue());

        aggDto.setStatistics(builder);
      }
    }
  }
}
