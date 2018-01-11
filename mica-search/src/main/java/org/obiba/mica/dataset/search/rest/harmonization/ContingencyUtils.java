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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class ContingencyUtils {

  private ContingencyUtils() {
  }

  public static List<String> getTermsHeaders(DatasetVariable variable, Mica.DatasetVariableContingenciesDto dto) {
    List<String> dtoTerms = Lists.newArrayList(
      dto.getContingenciesList().stream().flatMap(c -> c.getAggregationsList().stream()).map(a -> a.getTerm())
        .collect(toSet()));
    List<String> terms = variable.getCategories() != null ? variable.getCategories().stream().map(c -> c.getName())
      .collect(toList()) : Lists.newArrayList();
    terms.addAll(Sets.difference(Sets.newHashSet(dtoTerms), Sets.newHashSet(terms)));

    return terms;
  }

  public static List<String> getTermsHeaders(DatasetVariable variable, Mica.DatasetVariableContingencyDto dto) {
    List<String> terms = variable.getCategories() != null ? variable.getCategories().stream().map(c -> c.getName())
      .collect(toList()) : Lists.newArrayList();
    List<String> dtoTerms = dto.getAggregationsList().stream().map(a -> a.getTerm()).collect(Collectors.toList());
    terms.addAll(Sets.difference(Sets.newHashSet(dtoTerms), Sets.newHashSet(terms)));

    return terms;
  }

  public static List<String> getValuesHeaders(DatasetVariable variable, Mica.DatasetVariableContingenciesDto dto) {
    List<String> values = variable.getCategories() != null ? variable.getCategories().stream().map(c -> c.getName())
      .collect(toList()) : Lists.newArrayList();
    List<String> dtoValues = Lists.newArrayList(
      dto.getContingenciesList().stream().map(c -> c.getAll()).flatMap(a -> a.getFrequenciesList().stream())
        .map(f -> f.getValue()).collect(toSet()));
    values.addAll(Sets.difference(Sets.newHashSet(dtoValues), Sets.newHashSet(values)));

    return values;
  }

  public static List<String> getValuesHeaders(DatasetVariable variable, Mica.DatasetVariableContingencyDto dto) {
    List<String> values = variable.getCategories() != null ? variable.getCategories().stream().map(c -> c.getName())
      .collect(toList()) : Lists.newArrayList();
    List<String> dtoValues = dto.getAll().getFrequenciesList().stream().map(f -> f.getValue())
      .collect(Collectors.toList());
    values.addAll(Sets.difference(Sets.newHashSet(dtoValues), Sets.newHashSet(values)));

    return values;
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

  public static List<List<Number>> getContinuousRows(Mica.DatasetVariableContingencyDto c, List<String> terms) {
    List<Number> minList = Lists.newArrayList();
    List<Number> maxList = Lists.newArrayList();
    List<Number> meanList = Lists.newArrayList();
    List<Number> stdList = Lists.newArrayList();
    List<Number> nList = Lists.newArrayList();

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

  private static void addStats(Mica.StatisticsDto stats, List<Number> minList, List<Number> maxList, List<Number> meanList,
    List<Number> stdList) {
    minList.add(stats.getMin());
    maxList.add(stats.getMax());
    meanList.add(stats.getMean());
    stdList.add(stats.getStdDeviation());
  }
}
