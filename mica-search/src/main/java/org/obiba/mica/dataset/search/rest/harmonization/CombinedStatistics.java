/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class CombinedStatistics {
  private static final Logger log = LoggerFactory.getLogger(CombinedStatistics.class);

  private final List<Mica.StatisticsDto> stats = Lists.newArrayList();

  private final List<Integer> counts = Lists.newArrayList();

  CombinedStatistics(Collection<Mica.DatasetVariableAggregationDto> aggDtos) {
    aggDtos.stream().filter(aggDto -> aggDto.getN() > 0 && aggDto.hasStatistics()).forEach(a -> {
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

  public double getSum() {
    Optional<Double> result = stats.stream().filter(Mica.StatisticsDto::hasSum)
      .map(Mica.StatisticsDto::getSum).reduce((prev, curr) -> prev + curr);

    return result.orElse(0D);
  }

  public boolean hasMean() {
    return getCount() > 0;
  }

  public double getMean() {
    return hasMean() ? getSum() / getCount() : 0;
  }

  public boolean hasMin() {
    for(Mica.StatisticsDto stat : stats) {
      if(stat.hasMin() && stat.getMin() != Double.POSITIVE_INFINITY) return true;
    }
    return false;
  }

  public double getMin() {
    Optional<Double> result = stats.stream().filter(s -> s.hasMin() && s.getMin() != Double.POSITIVE_INFINITY)
      .map(Mica.StatisticsDto::getMin).reduce(Math::min);

    return result.orElse(0D);
  }

  public boolean hasMax() {
    for(Mica.StatisticsDto stat : stats) {
      if(stat.hasMax() && stat.getMax() != Double.NEGATIVE_INFINITY) return true;
    }
    return false;
  }

  public double getMax() {
    Optional<Double> result = stats.stream().filter(s -> s.hasMax() && s.getMax() != Double.NEGATIVE_INFINITY)
      .map(Mica.StatisticsDto::getMax).reduce(Math::max);

    return result.orElse(0D);
  }

  public boolean hasSumOfSquares() {
    for(Mica.StatisticsDto stat : stats) {
      if(stat.hasSumOfSquares()) return true;
    }
    return false;
  }

  public double getSumOfSquares() {
    Optional<Double> result = stats.stream().filter(Mica.StatisticsDto::hasSumOfSquares)
      .map(Mica.StatisticsDto::getSumOfSquares).reduce((prev, curr) -> prev + curr);

    return result.orElse(0D);
  }

  public double getVariance() {
    int count = getCount();

    // ESSG = error sum of squares within each group = variance * (n-1)
    // ESS = error sum of squares = sum(var(i) * (n(i)-1))
    double ess = IntStream.range(0, stats.size()).filter(i -> stats.get(i).hasVariance())
      .mapToDouble(i -> stats.get(i).getVariance() * (counts.get(i) - 1)).sum();

    // GM = grand mean = sum(mean(i) * n(i))
    double gm = IntStream.range(0, stats.size()).filter(i -> stats.get(i).hasMean())
      .mapToDouble(i -> stats.get(i).getMean() * counts.get(i)).sum() / count;

    // GSS = group sum of squares = sum(mean(i) - gm)^2 * n(i)
    double gss = IntStream.range(0, stats.size()).filter(i -> stats.get(i).hasMean())
      .mapToDouble(i -> Math.pow(stats.get(i).getMean() - gm, 2) * counts.get(i)).sum();

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
    aggDto.setN(aggDtos.stream().filter(a -> a.hasStatistics() || a.getFrequenciesCount()>0).mapToInt(Mica.DatasetVariableAggregationDto::getN).sum());
    aggDto.setTotal(aggDtos.stream().filter(a -> a.hasStatistics() || a.getFrequenciesCount()>0).mapToInt(Mica.DatasetVariableAggregationDto::getTotal).sum());
    aggDtos.forEach(a -> {
      if(a.hasTerm()) aggDto.setTerm(a.getTerm());
    });
  }

  private static void mergeFrequencies(Mica.DatasetVariableAggregationDto.Builder aggDto,
    Collection<Mica.DatasetVariableAggregationDto> aggDtos) {

    aggDtos.forEach(a -> a.getFrequenciesList().forEach(f -> {
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

    double gv = combined.getVariance();
    builder.setVariance(gv);
    double stdDeviation = Math.pow(gv, 0.5);

    if (Double.isNaN(stdDeviation)) {
      // TODO: remove this once the cause of the erronuous stdDev is found
      log.error("Invalid stdDeviation value with variance '{}', defaulting to zero.", gv);
      stdDeviation = 0.0D;
    }

    builder.setStdDeviation(stdDeviation);
    aggDto.setStatistics(builder);
  }
}
