package org.obiba.mica.dataset.search.rest.harmonized;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class ContingencyUtils {

  public static List<String> getTermsHeaders(Mica.DatasetVariableContingenciesDto dto) {
    Optional<Mica.DatasetVariableContingencyDto> contingency = dto.getContingenciesList().stream()
      .filter(c -> c.getAggregationsCount() > 0).findFirst();

    return contingency.isPresent() ? contingency.get().getAggregationsList().stream().map(a -> a.getTerm())
      .collect(toList()) : Lists.newArrayList();
  }

  public static List<String> getValuesHeaders(Mica.DatasetVariableContingenciesDto dto) {
    Optional<Mica.DatasetVariableAggregationDto> varAgg = dto.getContingenciesList().stream()
      .flatMap(c -> c.getAggregationsList().stream()).filter(a -> a.getFrequenciesCount() > 0).findFirst();

    return varAgg.isPresent()
      ? varAgg.get().getFrequenciesList().stream().map(f -> f.getValue()).collect(toList())
      : Lists.newArrayList();
  }

  public static List<List<Integer>> getCategoricalRows(Mica.DatasetVariableContingencyDto c, List<String> values,
    List<String> terms) {
    List<List<Integer>> res = Lists.newArrayList();
    Function<Mica.DatasetVariableAggregationDto, Map<String, Mica.FrequencyDto>> toFreqMap = a -> a.getFrequenciesList()
      .stream().collect(toMap(f -> f.getValue(), f -> f));
    Map<String, Map<String, Mica.FrequencyDto>> freqMap = c.getAggregationsList().stream()
      .collect(toMap(a -> a.getTerm(), a -> toFreqMap.apply(a)));
    Map<String, Mica.FrequencyDto> allFreqMap = c.getAll().getFrequenciesList().stream()
      .collect(toMap(f -> f.getValue(), f -> f));

    values.forEach(value -> {
      List<Integer> tmp = terms.stream().map(
        term -> freqMap.containsKey(term) && freqMap.get(term).containsKey(value) ? freqMap.get(term).get(value)
          .getCount() : 0).collect(toList());

      tmp.add(allFreqMap.containsKey(value) ? allFreqMap.get(value).getCount() : 0);
      res.add(tmp);
    });

    List<Integer> totals = c.getAggregationsList().stream().map(a -> a.getN()).collect(toList());
    totals.add(c.getAll().getN());
    res.add(totals);

    return res;
  }

  public static List<List<Float>> getContinuousRows(Mica.DatasetVariableContingencyDto c, List<String> terms) {
    List<Float> minList = Lists.newArrayList();
    List<Float> maxList = Lists.newArrayList();
    List<Float> meanList = Lists.newArrayList();
    List<Float> stdList = Lists.newArrayList();
    List<Float> nList = Lists.newArrayList();

    Map<String, Mica.DatasetVariableAggregationDto> map = c.getAggregationsList().stream()
      .collect(toMap(a -> a.getTerm(), a -> a));

    terms.forEach(t -> {
      if(map.containsKey(t)) addStats(map.get(t).getStatistics(), minList, maxList, meanList, stdList);

      nList.add((float) (map.containsKey(t) ? map.get(t).getN() : 0));
    });

    Mica.DatasetVariableAggregationDto a = c.getAll();
    if(a.hasStatistics()) addStats(a.getStatistics(), minList, maxList, meanList, stdList);
    nList.add((float) a.getN());

    return Lists.newArrayList(minList, maxList, meanList, stdList, nList);
  }

  private static void addStats(Mica.StatisticsDto stats, List<Float> minList, List<Float> maxList, List<Float> meanList,
    List<Float> stdList) {
    minList.add(stats.getMin());
    maxList.add(stats.getMax());
    meanList.add(stats.getMean());
    stdList.add(stats.getStdDeviation());
  }

  public static boolean checkIsContinuous(Mica.DatasetVariableContingencyDto dto) {
    return dto.getAll().hasStatistics() || dto.getAggregationsCount() == 0;
  }
}
