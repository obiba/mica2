package org.obiba.mica.dataset.search.rest.harmonized;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

public class CombinedStatistics {

  private final List<Mica.StatisticsDto> stats = Lists.newArrayList();

  private final List<Integer> counts = Lists.newArrayList();

  CombinedStatistics(Collection<Mica.DatasetVariableAggregationDto> aggDtos) {
    aggDtos.stream().filter(Mica.DatasetVariableAggregationDto::hasStatistics).forEach(a -> {
      stats.add(a.getStatistics());
      counts.add(a.getN());
    });
  }

  public boolean hasStatistics() {
    return !stats.isEmpty();
  }

  public int getCount() {
    return counts.stream().mapToInt(c -> c).sum();
  }

  public float getSum() {
    return Double
      .valueOf(stats.stream().filter(Mica.StatisticsDto::hasSum).mapToDouble(Mica.StatisticsDto::getSum).sum())
      .floatValue();
  }

  public boolean hasMean() {
    return getCount() > 0;
  }

  public float getMean() {
    return hasMean() ? getSum() / getCount() : 0;
  }

  public boolean hasMin() {
    for(Mica.StatisticsDto stat : stats) {
      if(stat.hasMin() && stat.getMin() != Float.POSITIVE_INFINITY) return true;
    }
    return false;
  }

  public float getMin() {
    return Double.valueOf(stats.stream().filter(s -> s.hasMin() && s.getMin() != Float.POSITIVE_INFINITY)
      .mapToDouble(Mica.StatisticsDto::getMin).reduce(0, Math::min)).floatValue();
  }

  public boolean hasMax() {
    for(Mica.StatisticsDto stat : stats) {
      if(stat.hasMax() && stat.getMax() != Float.NEGATIVE_INFINITY) return true;
    }
    return false;
  }

  public float getMax() {
    return Double.valueOf(stats.stream().filter(s -> s.hasMax() && s.getMax() != Float.NEGATIVE_INFINITY)
      .mapToDouble(Mica.StatisticsDto::getMax).reduce(0, Math::max)).floatValue();
  }

  public boolean hasSumOfSquares() {
    for(Mica.StatisticsDto stat : stats) {
      if(stat.hasSumOfSquares()) return true;
    }
    return false;
  }

  public float getSumOfSquares() {
    return Double.valueOf(
      stats.stream().filter(Mica.StatisticsDto::hasSumOfSquares).mapToDouble(Mica.StatisticsDto::getSumOfSquares).sum())
      .floatValue();
  }

  public float getVariance() {
    int count = getCount();

    // ESSG = error sum of squares within each group = variance * (n-1)
    // ESS = error sum of squares = sum(var(i) * (n(i)-1))
    float ess = Double.valueOf(IntStream.range(0, stats.size()).filter(i -> stats.get(i).hasVariance())
      .mapToDouble(i -> stats.get(i).getVariance() * (counts.get(i) - 1)).sum()).floatValue();

    // GM = grand mean = sum(mean(i) * n(i))
    float gm = Double.valueOf(IntStream.range(0, stats.size()).filter(i -> stats.get(i).hasMean())
      .mapToDouble(i -> stats.get(i).getMean() * counts.get(i)).sum() / count).floatValue();

    // GSS = group sum of squares = sum(mean(i) - gm)^2 * n(i)
    float gss = Double.valueOf(IntStream.range(0, stats.size()).filter(i -> stats.get(i).hasMean())
      .mapToDouble(i -> Math.pow(stats.get(i).getMean() - gm, 2) * counts.get(i)).sum()).floatValue();

    // GV = grand variance
    return count == 1 ? 0 : (ess - gss) / (count - 1);
  }

  //
  // Static methods
  //

  public static Mica.DatasetVariableAggregationDto mergeAggregations(
    Collection<Mica.DatasetVariableAggregationDto> aggDtos) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    mergeAggregations(aggDto, aggDtos);
    return aggDto.build();
  }

  //
  // Private methods
  //

  private static void mergeAggregations(Mica.DatasetVariableAggregationDto.Builder aggDto,
    Collection<Mica.DatasetVariableAggregationDto> aggDtos) {
    mergeFrequencies(aggDto, aggDtos);
    mergeStatistics(aggDto, aggDtos);
    aggDto.setN(aggDtos.stream().mapToInt(Mica.DatasetVariableAggregationDto::getN).sum());
    aggDto.setTotal(aggDtos.stream().mapToInt(Mica.DatasetVariableAggregationDto::getTotal).sum());
    aggDtos.forEach(a -> {
      if(a.hasTerm()) aggDto.setTerm(a.getTerm());
    });
  }

  private static void mergeFrequencies(Mica.DatasetVariableAggregationDto.Builder aggDto,
    Collection<Mica.DatasetVariableAggregationDto> aggDtos) {

    aggDtos.stream().forEach(a -> a.getFrequenciesList().forEach(f -> {
      boolean found = false;
      for(int i = 0; i < aggDto.getFrequenciesCount(); i++) {
        Mica.FrequencyDto freq = aggDto.getFrequencies(i);
        if(freq.getValue().equals(f.getValue())) {
          aggDto.setFrequencies(i, freq.toBuilder().setCount(freq.getCount() + f.getCount()).build());
          found = true;
          break;
        }
      }
      if(!found) {
        aggDto.addFrequencies(Mica.FrequencyDto.newBuilder(f));
      }
    }));
  }

  private static void mergeStatistics(Mica.DatasetVariableAggregationDto.Builder aggDto,
    Collection<Mica.DatasetVariableAggregationDto> aggDtos) {

    CombinedStatistics combined = new CombinedStatistics(aggDtos);
    if(!combined.hasStatistics()) return;

    Mica.StatisticsDto.Builder builder = Mica.StatisticsDto.newBuilder();
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
