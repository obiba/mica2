/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonized;

import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Dataschema variable resource: variable describing an harmonization dataset.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedDataschemaDatasetVariableResource extends AbstractPublishedDatasetResource<HarmonizationDataset>
  implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedDataschemaDatasetVariableResource.class);

  private String datasetId;

  private String variableName;

  @Inject
  private HarmonizationDatasetService datasetService;

  @Inject
  private Helper helper;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Dataschema);
  }

  @GET
  @Path("/summary")
  public List<Math.SummaryStatisticsDto> getVariableSummaries() {
    ImmutableList.Builder<Math.SummaryStatisticsDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    dataset.getStudyTables().forEach(table -> {
      try {
        builder.add(datasetService
          .getVariableSummary(dataset, variableName, table.getStudyId(), table.getProject(), table.getTable()));
      } catch(NoSuchVariableException | NoSuchValueTableException e) {
        // case the study has not implemented this dataschema variable
        builder.add(Math.SummaryStatisticsDto.newBuilder().setResource(variableName).build());
      }
    });
    return builder.build();
  }

  @GET
  @Path("/facet")
  public List<Search.QueryResultDto> getVariableFacets() {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    dataset.getStudyTables().forEach(table -> {
      try {
        builder.add(datasetService.getVariableFacet(dataset, variableName, table.getStudyId(), table.getProject(), table.getTable()));
      } catch(NoSuchVariableException | NoSuchValueTableException e) {
        // case the study has not implemented this dataschema variable
        builder.add(Search.QueryResultDto.newBuilder().setTotalHits(0).build());
      }
    });
    return builder.build();
  }

  @GET
  @Path("/aggregation")
  public Mica.DatasetVariableAggregationsDto getVariableAggregations() {
    ImmutableList.Builder<Mica.DatasetVariableAggregationDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    Mica.DatasetVariableAggregationsDto.Builder aggDto = Mica.DatasetVariableAggregationsDto.newBuilder();

    List<Future<Math.SummaryStatisticsDto>> results = Lists.newArrayList();
    dataset.getStudyTables().forEach(table -> results.add(helper.getVariableFacet(dataset, variableName, table)));

    for(int i = 0; i < dataset.getStudyTables().size(); i++) {
      StudyTable table = dataset.getStudyTables().get(i);
      Future<Math.SummaryStatisticsDto> futureResult = results.get(i);
      try {
        Mica.DatasetVariableAggregationDto tableAggDto = dtos.asDto(table, futureResult.get()).build();
        builder.add(tableAggDto);
        mergeAggregations(aggDto, tableAggDto);
      } catch(Exception e) {
        log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
        builder.add(dtos.asDto(table, null).build());
      }
    }

    aggDto.addAllAggregations(builder.build());

    return aggDto.build();
  }

  /**
   * Get the harmonized variable summaries for each of the study.
   *
   * @return
   */
  @GET
  @Path("/harmonizations")
  public Mica.DatasetVariableHarmonizationDto getVariableHarmonizations() {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    return getVariableHarmonizationDto(dataset, variableName);
  }

  @Override
  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  @Override
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  private void mergeAggregations(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    mergeAggregationFrequencies(aggDto, tableAggDto);
    mergeAggregationStatistics(aggDto, tableAggDto);
    aggDto.setTotal(aggDto.getTotal() + tableAggDto.getTotal());
    aggDto.setN(aggDto.getN() + tableAggDto.getN());
  }

  private void mergeAggregationFrequencies(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    if(tableAggDto.getFrequenciesCount() == 0) return;

    for(Mica.DatasetVariableAggregationDto.FrequencyDto tableFreq : tableAggDto.getFrequenciesList()) {
      boolean found = false;
      for(int i = 0; i < aggDto.getFrequenciesCount(); i++) {
        Mica.DatasetVariableAggregationDto.FrequencyDto freq = aggDto.getFrequencies(i);
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

  private void mergeAggregationStatistics(Mica.DatasetVariableAggregationsDto.Builder aggDto,
    Mica.DatasetVariableAggregationDto tableAggDto) {
    if(!tableAggDto.hasStatistics()) return;

    if(!aggDto.hasStatistics()) {
      aggDto.setStatistics(tableAggDto.getStatistics().toBuilder());
    } else {

      Mica.DatasetVariableAggregationDto.StatisticsDto stats = aggDto.getStatistics();
      Mica.DatasetVariableAggregationDto.StatisticsDto tableStats = tableAggDto.getStatistics();

      int count = aggDto.getN() + tableAggDto.getN();

      if (count > 0) {
        Mica.DatasetVariableAggregationDto.StatisticsDto.Builder builder = aggDto.getStatistics().toBuilder();

        float sum = (stats.hasSum() ? stats.getSum() : 0) + (tableStats.hasSum() ? tableStats.getSum() : 0);
        builder.setSum(sum);
        float mean = 0;
        if(count > 0) builder.setMean(mean = sum / count);

        if(tableStats.hasMin() && tableStats.getMin() != Float.POSITIVE_INFINITY) {
          builder.setMin(stats.hasMin() ? java.lang.Math.min(stats.getMin(), tableStats.getMin()) : tableStats.getMin());
        }
        if(tableStats.hasMax() && tableStats.getMax() != Float.NEGATIVE_INFINITY) {
          builder.setMax(stats.hasMax() ? java.lang.Math.max(stats.getMax(), tableStats.getMax()) : tableStats.getMax());
        }
        if(tableStats.hasSumOfSquares()) {
          builder
            .setSumOfSquares((stats.hasSumOfSquares() ? 0 : stats.getSumOfSquares()) + tableStats.getSumOfSquares());
        }

        // ESSG = error sum of squares within each group = variance * (n-1)
        // ESS = error sum of squares = sum(var(i) * (n(i)-1))
        float essg = (stats.hasVariance() ? stats.getVariance() : 0) * (aggDto.getN() - 1);
        float tableEssg = (tableStats.hasVariance() ? tableStats.getVariance() : 0) * (tableAggDto.getN() - 1);
        float ess = essg + tableEssg;

        // GM = grand mean = sum(n(i) * mean(i))
        float tableMean = tableStats.hasMean() ? tableStats.getMean() : 0;
        float gm = (mean * aggDto.getN() + tableMean * tableAggDto.getN()) / count;

        // GSS = group sum of squares = (mean(i) - gm)^2 * n(i)
        float gss = Double.valueOf(java.lang.Math.pow(mean - gm, 2)).floatValue() * aggDto.getN();
        float tableGss = Double.valueOf(java.lang.Math.pow(tableMean - gm, 2)).floatValue() * tableAggDto.getN();
        float tgss = gss + tableGss;

        // GV = grand variance
        float gv = count == 1 ? 0 :(ess - tgss) / (count - 1);
        builder.setVariance(gv);
        builder.setStdDeviation(Double.valueOf(java.lang.Math.pow(gv, 0.5)).floatValue());

        aggDto.setStatistics(builder);
      }
    }
  }

  @Component
  public static class Helper {
    @Inject
    private HarmonizationDatasetService datasetService;

    @Async
    private Future<Math.SummaryStatisticsDto> getVariableFacet(HarmonizationDataset dataset, String variableName, StudyTable table) {
      try {
        return new AsyncResult<>(datasetService.getVariableSummary(dataset, variableName, table.getStudyId(), table.getProject(), table.getTable()));
      } catch(Exception e) {
        log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
        return new AsyncResult<>(null);
      }
    }
  }
}
