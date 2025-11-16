/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.source.support;

import com.google.common.collect.Lists;
import org.obiba.magma.type.BooleanType;
import org.obiba.mica.spi.tables.ICategory;
import org.obiba.mica.spi.tables.IVariable;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class OpalDtos {

  //
  // Search.QueryResultDto
  //

  public static Mica.DatasetVariableContingencyDto asDto(IVariable variable, IVariable crossVariable, int privacyThreshold, Search.QueryResultDto results) {
    Mica.DatasetVariableContingencyDto.Builder crossDto = Mica.DatasetVariableContingencyDto.newBuilder();
    Mica.DatasetVariableAggregationDto.Builder allAggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();

    if (results == null) {
      allAggBuilder.setN(0);
      allAggBuilder.setTotal(0);
      crossDto.setAll(allAggBuilder);
      return crossDto.build();
    }

    allAggBuilder.setTotal(results.getTotalHits());
    crossDto.setPrivacyThreshold(privacyThreshold);
    boolean privacyChecks = !crossVariable.hasCategories() || validatePrivacyThreshold(results, privacyThreshold);
    boolean totalPrivacyChecks = validateTotalPrivacyThreshold(results, privacyThreshold);

    // add facet results in the same order as the variable categories
    List<String> catNames = variable.getValueType().equals(BooleanType.get().getName()) ?
      Lists.newArrayList("true", "false") : variable.getCategoryNames();
    catNames.forEach(catName -> results.getFacetsList().stream()
      .filter(facet -> facet.hasFacet() && catName.equals(facet.getFacet())).forEach(facet -> {
        boolean privacyCheck = privacyChecks && checkPrivacyThreshold(facet.getFilters(0).getCount(), privacyThreshold);
        Mica.DatasetVariableAggregationDto.Builder aggBuilder = Mica.DatasetVariableAggregationDto.newBuilder();
        aggBuilder.setTotal(totalPrivacyChecks ? results.getTotalHits() : 0);
        aggBuilder.setTerm(facet.getFacet());
        ICategory category = variable.getCategory(facet.getFacet());
        aggBuilder.setMissing(category != null && category.isMissing());
        addSummaryStatistics(crossVariable, aggBuilder, facet, privacyCheck, totalPrivacyChecks);
        crossDto.addAggregations(aggBuilder);
      }));

    // add total facet for all variable categories
    results.getFacetsList().stream().filter(facet -> facet.hasFacet() && "_total".equals(facet.getFacet()))
      .forEach(facet -> {
        boolean privacyCheck = privacyChecks && facet.getFilters(0).getCount() >= privacyThreshold;
        addSummaryStatistics(crossVariable, allAggBuilder, facet, privacyCheck, totalPrivacyChecks);
      });

    crossDto.setAll(allAggBuilder);

    return crossDto.build();

  }

  private static boolean checkPrivacyThreshold(int count, int threshold) {
    return count == 0 || count >= threshold;
  }

  private static boolean validateTotalPrivacyThreshold(Search.QueryResultDtoOrBuilder results, int privacyThreshold) {
    return results.getFacetsList().stream()
      .allMatch(facet -> checkPrivacyThreshold(facet.getFilters(0).getCount(), privacyThreshold));
  }

  private static boolean validatePrivacyThreshold(Search.QueryResultDtoOrBuilder results, int privacyThreshold) {
    return results.getFacetsList().stream().map(Search.FacetResultDto::getFrequenciesList).flatMap(Collection::stream)
      .allMatch(freq -> checkPrivacyThreshold(freq.getCount(), privacyThreshold));
  }

  private static void addSummaryStatistics(IVariable crossVariable,
                                           Mica.DatasetVariableAggregationDto.Builder aggBuilder, Search.FacetResultDto facet, boolean privacyCheck,
                                           boolean totalPrivacyCheck) {
    aggBuilder.setN(totalPrivacyCheck ? facet.getFilters(0).getCount() : -1);
    if (!privacyCheck) return;

    List<String> catNames = crossVariable.getValueType().equals(BooleanType.get().getName()) ?
      Lists.newArrayList("1", "0") : crossVariable.getCategoryNames();
    // order results as the order of cross variable categories
    catNames.forEach(catName -> facet.getFrequenciesList().stream().filter(freq -> catName.equals(freq.getTerm()))
      .forEach(freq -> aggBuilder.addFrequencies(asDto(crossVariable, freq))));
    // observed terms, not described by categories
    facet.getFrequenciesList().stream().filter(freq -> !catNames.contains(freq.getTerm()))
      .forEach(freq -> aggBuilder.addFrequencies(asDto(crossVariable, freq)));

    if (facet.hasStatistics()) {
      aggBuilder.setStatistics(asDto(facet.getStatistics()));
    }
  }

  private static Mica.FrequencyDto.Builder asDto(IVariable crossVariable,
                                                 Search.FacetResultDto.TermFrequencyResultDto result) {
    if (crossVariable.getValueType().equals(BooleanType.get().getName())) {
      // for some reason 0/1 is returned instead of false/true
      return Mica.FrequencyDto.newBuilder()
        .setValue("1".equals(result.getTerm()) ? "true" : "false")
        .setCount(result.getCount())
        .setMissing(false);
    } else if (crossVariable.getCategory(result.getTerm()) != null) {
      ICategory category = crossVariable.getCategory(result.getTerm());
      return Mica.FrequencyDto.newBuilder()
        .setValue(result.getTerm())
        .setCount(result.getCount())
        .setMissing(category != null && category.isMissing());
    } else {
      // observed value, not described by a category
      return Mica.FrequencyDto.newBuilder()
        .setValue(result.getTerm())
        .setCount(result.getCount())
        .setMissing(false);
    }
  }

  private static Mica.StatisticsDto.Builder asDto(Search.FacetResultDto.StatisticalResultDto result) {
    return Mica.StatisticsDto.newBuilder() //
      .setMin(result.getMin()) //
      .setMax(result.getMax()) //
      .setMean(result.getMean()) //
      .setSum(result.getTotal()) //
      .setSumOfSquares(result.getSumOfSquares()) //
      .setVariance(result.getVariance()) //
      .setStdDeviation(result.getStdDeviation());
  }

  //
  // SummaryStatisticsDto methods
  //

  public static Mica.DatasetVariableAggregationDto asDto(@Nullable Math.SummaryStatisticsDto summary) {

    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();

    if (summary == null) return aggDto.setTotal(0).setN(0).build();

    if (summary.hasExtension(Math.CategoricalSummaryDto.categorical)) {
      aggDto = asDto(summary.getExtension(Math.CategoricalSummaryDto.categorical));
    } else if (summary.hasExtension(Math.ContinuousSummaryDto.continuous)) {
      aggDto = asDto(summary.getExtension(Math.ContinuousSummaryDto.continuous));
    } else if (summary.hasExtension(Math.DefaultSummaryDto.defaultSummary)) {
      aggDto = asDto(summary.getExtension(Math.DefaultSummaryDto.defaultSummary));
    } else if (summary.hasExtension(Math.TextSummaryDto.textSummary)) {
      aggDto = asDto(summary.getExtension(Math.TextSummaryDto.textSummary));
    } else if (summary.hasExtension(Math.GeoSummaryDto.geoSummary)) {
      aggDto = asDto(summary.getExtension(Math.GeoSummaryDto.geoSummary));
    } else if (summary.hasExtension(Math.BinarySummaryDto.binarySummary)) {
      aggDto = asDto(summary.getExtension(Math.BinarySummaryDto.binarySummary));
    }

    return aggDto.build();
  }

  private static Mica.DatasetVariableAggregationDto.Builder asDto(Math.CategoricalSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList(),
      summary.hasOtherFrequency() ? Long.valueOf(summary.getOtherFrequency()).intValue() : 0);
    return aggDto;
  }

  private static Mica.DatasetVariableAggregationDto.Builder asDto(Math.DefaultSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private static Mica.DatasetVariableAggregationDto.Builder asDto(Math.TextSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList(),
      summary.hasOtherFrequency() ? Long.valueOf(summary.getOtherFrequency()).intValue() : 0);
    return aggDto;
  }

  private static Mica.DatasetVariableAggregationDto.Builder asDto(Math.GeoSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private static Mica.DatasetVariableAggregationDto.Builder asDto(Math.BinarySummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    aggDto.setTotal(Long.valueOf(summary.getN()).intValue());
    addFrequenciesDto(aggDto, summary.getFrequenciesList());
    return aggDto;
  }

  private static Mica.FrequencyDto.Builder asDto(Math.FrequencyDto freq) {
    return Mica.FrequencyDto.newBuilder().setValue(freq.getValue()).setCount(Long.valueOf(freq.getFreq()).intValue())
      .setMissing(freq.getMissing());
  }

  private static Mica.IntervalFrequencyDto.Builder asDto(Math.IntervalFrequencyDto inter) {
    return Mica.IntervalFrequencyDto.newBuilder().setCount((int) inter.getFreq())
      .setLower(inter.getLower()).setUpper(inter.getUpper());
  }

  private static void addFrequenciesDto(Mica.DatasetVariableAggregationDto.Builder aggDto,
                                        List<Math.FrequencyDto> frequencies) {
    addFrequenciesDto(aggDto, frequencies, 0);
  }

  private static void addFrequenciesDto(Mica.DatasetVariableAggregationDto.Builder aggDto, List<Math.FrequencyDto> frequencies,
                                        int otherFrequency) {
    int n = otherFrequency;
    if (frequencies != null) {
      for (Math.FrequencyDto freq : frequencies) {
        aggDto.addFrequencies(asDto(freq));
        if (!freq.getMissing()) n += freq.getFreq();
      }
    }
    if (otherFrequency > 0)
      aggDto.addFrequencies(Mica.FrequencyDto.newBuilder().setValue("???").setCount(otherFrequency)
        .setMissing(false));
    aggDto.setN(n);
  }

  private static Mica.DatasetVariableAggregationDto.Builder asDto(Math.ContinuousSummaryDto summary) {
    Mica.DatasetVariableAggregationDto.Builder aggDto = Mica.DatasetVariableAggregationDto.newBuilder();
    Math.DescriptiveStatsDto stats = summary.getSummary();

    aggDto.setN(Long.valueOf(stats.getN()).intValue());

    Mica.StatisticsDto.Builder builder = Mica.StatisticsDto.newBuilder();

    if (stats.hasSum()) builder.setSum(Double.valueOf(stats.getSum()).floatValue());
    if (stats.hasMin() && stats.getMin() != Double.POSITIVE_INFINITY)
      builder.setMin(Double.valueOf(stats.getMin()).floatValue());
    if (stats.hasMax() && stats.getMax() != Double.NEGATIVE_INFINITY)
      builder.setMax(Double.valueOf(stats.getMax()).floatValue());
    if (stats.hasMean() && !Double.isNaN(stats.getMean()))
      builder.setMean(Double.valueOf(stats.getMean()).floatValue());
    if (stats.hasSumsq() && !Double.isNaN(stats.getSumsq()))
      builder.setSumOfSquares(Double.valueOf(stats.getSumsq()).floatValue());
    if (stats.hasVariance() && !Double.isNaN(stats.getVariance()))
      builder.setVariance(Double.valueOf(stats.getVariance()).floatValue());
    if (stats.hasStdDev() && !Double.isNaN(stats.getStdDev()))
      builder.setStdDeviation(Double.valueOf(stats.getStdDev()).floatValue());

    aggDto.setStatistics(builder);

    if (summary.getFrequenciesCount() > 0) {
      summary.getFrequenciesList().forEach(freq -> aggDto.addFrequencies(asDto(freq)));
    }

    if (summary.getIntervalFrequencyCount() > 0) {
      summary.getIntervalFrequencyList().forEach(inter -> aggDto.addIntervalFrequencies(asDto(inter)));
    }

    int total = 0;
    if (summary.getFrequenciesCount() > 0) {
      for (Math.FrequencyDto freq : summary.getFrequenciesList()) {
        total += freq.getFreq();
      }
    }
    aggDto.setTotal(total);

    return aggDto;
  }

}
