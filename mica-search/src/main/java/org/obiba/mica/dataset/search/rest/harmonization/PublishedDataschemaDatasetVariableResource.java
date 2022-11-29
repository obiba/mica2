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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.math3.util.Pair;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Dataschema variable resource: variable describing an harmonization dataset.
 */
@Component
@Scope("request")
public class PublishedDataschemaDatasetVariableResource extends AbstractPublishedDatasetResource<HarmonizationDataset>
  implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedDataschemaDatasetVariableResource.class);

  private String datasetId;

  private String variableName;

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private Helper helper;

  @GET
  @Timed
  public Mica.DatasetVariableDto getVariable() {
    checkDatasetAccess();
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Dataschema);
  }

  @GET
  @Path("/summary")
  @Timed
  public List<Math.SummaryStatisticsDto> getVariableSummaries() {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    ImmutableList.Builder<Math.SummaryStatisticsDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    dataset.getBaseStudyTables().forEach(table -> {
      try {
        String studyId = table.getStudyId();
        builder.add(datasetService
          .getVariableSummary(dataset, variableName, studyId, table.getSourceURN())
          .getWrappedDto());
      } catch (NoSuchVariableException | NoSuchValueTableException e) {
        // case the study has not implemented this dataschema variable
        builder.add(Math.SummaryStatisticsDto.newBuilder().setResource(variableName).build());
      }
    });
    return builder.build();
  }

  @GET
  @Path("/aggregation")
  @Timed
  public Mica.DatasetVariableAggregationsDto getVariableAggregations(@QueryParam("study") @DefaultValue("true") boolean withStudySummary) {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    ImmutableList.Builder<Mica.DatasetVariableAggregationDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    Mica.DatasetVariableAggregationsDto.Builder aggDto = Mica.DatasetVariableAggregationsDto.newBuilder();

    List<Future<Math.SummaryStatisticsDto>> results = Lists.newArrayList();
    dataset.getBaseStudyTables().forEach(table -> results.add(helper.getVariableFacet(dataset, variableName, table)));

    for (int i = 0; i < dataset.getBaseStudyTables().size(); i++) {
      BaseStudyTable opalTable = dataset.getBaseStudyTables().get(i);
      Future<Math.SummaryStatisticsDto> futureResult = results.get(i);
      try {
        builder.add(dtos.asDto(opalTable, futureResult.get(), withStudySummary).build());
      } catch (Exception e) {
        log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
        builder.add(dtos.asDto(opalTable, null, withStudySummary).build());
      }
    }

    List<Mica.DatasetVariableAggregationDto> aggsDto = builder.build();
    Mica.DatasetVariableAggregationDto allAggDto = CombinedStatistics.mergeAggregations(aggsDto);
    aggDto.setN(allAggDto.getN());
    aggDto.setTotal(allAggDto.getTotal());
    if (allAggDto.hasStatistics()) aggDto.setStatistics(allAggDto.getStatistics());
    aggDto.addAllFrequencies(allAggDto.getFrequenciesList());

    aggDto.addAllAggregations(aggsDto);

    return aggDto.build();
  }

  @GET
  @Path("/contingency")
  @Timed
  public Mica.DatasetVariableContingenciesDto getContingency(@QueryParam("by") String crossVariable) {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    checkContingencyAccess();
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);
    return getDatasetVariableContingenciesDto(variables.getFirst(), variables.getSecond());
  }

  @GET
  @Path("/contingency/_export")
  @Produces("text/csv")
  @Timed
  public Response getContingencyCsv(@QueryParam("by") String crossVariable) throws IOException {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    checkContingencyAccess();
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);
    ByteArrayOutputStream value = new CsvContingencyWriter(variables.getFirst(), variables.getSecond())
      .write(getDatasetVariableContingenciesDto(variables.getFirst(), variables.getSecond()));

    return Response.ok(value.toByteArray()).header("Content-Disposition",
      String.format("attachment; filename=\"contingency-table-%s-%s.csv\"", variableName, crossVariable)).build();
  }

  @GET
  @Path("/contingency/_export")
  @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @Timed
  public Response getContingencyExcel(@QueryParam("by") String crossVariable) throws IOException {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    checkContingencyAccess();
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);
    ByteArrayOutputStream value = new ExcelContingencyWriter(variables.getFirst(), variables.getSecond())
      .write(getDatasetVariableContingenciesDto(variables.getFirst(), variables.getSecond()));

    return Response.ok(value.toByteArray()).header("Content-Disposition",
      String.format("attachment; filename=\"contingency-table-%s-%s.xlsx\"", variableName, crossVariable)).build();
  }

  /**
   * Get the harmonized variable summaries for each of the study.
   *
   * @return
   */
  @GET
  @Path("/harmonizations")
  @Timed
  public Mica.DatasetVariableHarmonizationDto getVariableHarmonizations() {
    checkDatasetAccess();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    return getVariableHarmonizationDto(dataset, variableName, true);
  }

  @Override
  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  @Override
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  //
  // Private methods
  //

  private Mica.DatasetVariableContingenciesDto getDatasetVariableContingenciesDto(DatasetVariable var,
                                                                                  DatasetVariable crossVar) {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    Mica.DatasetVariableContingenciesDto.Builder crossDto = Mica.DatasetVariableContingenciesDto.newBuilder();

    List<Future<Search.QueryResultDto>> results = Lists.newArrayList();
    dataset.getBaseStudyTables().forEach(table -> results.add(helper.getContingencyTable(dataset, var, crossVar, table)));

    Multimap<String, Mica.DatasetVariableAggregationDto> termAggregations = LinkedListMultimap.create();

    for (int i = 0; i < dataset.getBaseStudyTables().size(); i++) {
      BaseStudyTable opalTable = dataset.getBaseStudyTables().get(i);
      Future<Search.QueryResultDto> futureResult = results.get(i);

      try {
        Mica.DatasetVariableContingencyDto studyTableCrossDto = dtos
          .asContingencyDto(opalTable, var, crossVar, futureResult.get()).build();
        termAggregations.put(null, studyTableCrossDto.getAll());
        studyTableCrossDto.getAggregationsList()
          .forEach(termAggDto -> termAggregations.put(termAggDto.getTerm(), termAggDto));
        crossDto.addContingencies(studyTableCrossDto);
      } catch (Exception e) {
        log.warn("Unable to retrieve contingency table: " + e.getMessage(), e);
        crossDto.addContingencies(dtos.asContingencyDto(opalTable, var, crossVar, null));
      }
    }

    // Merge aggregations by term (=variable category) + all terms aggregation.
    Mica.DatasetVariableContingencyDto.Builder allContingencies = Mica.DatasetVariableContingencyDto.newBuilder();
    termAggregations.asMap().entrySet().forEach(entry -> {
      Mica.DatasetVariableAggregationDto merged = CombinedStatistics.mergeAggregations(entry.getValue());
      if (entry.getKey() == null) {
        allContingencies.setAll(merged);
      } else {
        allContingencies.addAggregations(merged);
      }
    });

    crossDto.setAll(allContingencies);

    return crossDto.build();
  }

  private Pair<DatasetVariable, DatasetVariable> getContingencyVariables(String crossVariable) {
    if (Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Cross variable name is required for the contingency table");

    DatasetVariable var = getDatasetVariable(datasetId, variableName, DatasetVariable.Type.Dataschema, null);
    DatasetVariable crossVar = getDatasetVariable(datasetId, crossVariable, DatasetVariable.Type.Dataschema, null);

    return Pair.create(var, crossVar);
  }

  private void checkDatasetAccess() {
    subjectAclService.checkAccess("/harmonized-dataset", datasetId);
  }

  @Component
  public static class Helper {

    @Inject
    protected HarmonizedDatasetService datasetService;

    @Async
    protected Future<Math.SummaryStatisticsDto> getVariableFacet(HarmonizationDataset dataset, String variableName,
                                                                 BaseStudyTable table) {
      try {
        String studyId = table.getStudyId();
        return new AsyncResult<>(datasetService
          .getVariableSummary(dataset, variableName, studyId, table.getSourceURN())
          .getWrappedDto());
      } catch (Exception e) {
        log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
        return new AsyncResult<>(null);
      }
    }

    @Async
    protected Future<Search.QueryResultDto> getContingencyTable(HarmonizationDataset dataset, DatasetVariable var,
                                                                DatasetVariable crossVar, BaseStudyTable studyTable) {
      try {
        return new AsyncResult<>(datasetService.getContingencyTable(studyTable, var, crossVar));
      } catch (Exception e) {
        log.warn("Unable to retrieve contingency statistics: " + e.getMessage(), e);
        return new AsyncResult<>(null);
      }
    }
  }
}
