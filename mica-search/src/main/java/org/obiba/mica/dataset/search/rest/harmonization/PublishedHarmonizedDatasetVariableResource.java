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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import org.apache.commons.math3.util.Pair;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Harmonized variable resource: variable of an harmonization dataset, implementing the dataschema variable with the same name.
 */
@Component
@Scope("request")
public class PublishedHarmonizedDatasetVariableResource extends AbstractPublishedDatasetResource<HarmonizationDataset>
  implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizedDatasetVariableResource.class);

  private String datasetId;

  private String variableName;

  private String studyId;

  private String project;

  private String table;

  private String tableType;

  @Inject
  private HarmonizedDatasetService datasetService;

  @GET
  @Timed
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Harmonized, studyId, project, table,
      tableType);
  }

  @GET
  @Path("/summary")
  @Timed
  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary() {
    return datasetService
      .getVariableSummary(getDataset(HarmonizationDataset.class, datasetId), variableName, studyId, project, table)
      .getWrappedDto();
  }

  @GET
  @Path("/facet")
  @Timed
  public Search.QueryResultDto getVariableFacet() {
    return datasetService
      .getVariableFacet(getDataset(HarmonizationDataset.class, datasetId), variableName, studyId, project, table);
  }

  @GET
  @Path("/aggregation")
  @Timed
  public Mica.DatasetVariableAggregationDto getVariableAggregations(@QueryParam("study") @DefaultValue("true") boolean withStudySummary) {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    for(BaseStudyTable opalTable : dataset.getBaseStudyTables()) {
      String opalTableId = studyId;
      if(opalTable.isFor(opalTableId, project, table)) {
        try {
          return dtos.asDto(opalTable,
            datasetService.getVariableSummary(dataset, variableName, studyId, project, table).getWrappedDto(), withStudySummary).build();
        } catch(Exception e) {
          log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
          return dtos.asDto(opalTable, null, withStudySummary).build();
        }
      }
    }

    throw new NoSuchValueTableException(project, table);
  }

  @GET
  @Path("/contingency")
  @Timed
  public Mica.DatasetVariableContingencyDto getContingency(@QueryParam("by") String crossVariable) {
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);

    return getContingencyDto(variables.getFirst(), variables.getSecond());
  }

  private Mica.DatasetVariableContingencyDto getContingencyDto(DatasetVariable var, DatasetVariable crossVar) {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);

    for(BaseStudyTable opalTable : dataset.getBaseStudyTables()) {
      String opalTableId = studyId;
      if(opalTable.isFor(opalTableId, project, table)) {
        try {
          return dtos.asContingencyDto(opalTable, var, crossVar,
            datasetService.getContingencyTable(opalTable, var, crossVar)).build();
        } catch(Exception e) {
          log.warn("Unable to retrieve contingency table: " + e.getMessage(), e);
          return dtos.asContingencyDto(opalTable, var, crossVar, null).build();
        }
      }
    }

    throw new NoSuchValueTableException(project, table);
  }

  @GET
  @Path("/contingency/_export")
  @Produces("text/csv")
  @Timed
  public Response getContingencyCsv(@QueryParam("by") String crossVariable) throws IOException {
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);
    ByteArrayOutputStream res = new CsvContingencyWriter(variables.getFirst(), variables.getSecond())
      .write(getContingencyDto(variables.getFirst(), variables.getSecond()));

    return Response.ok(res.toByteArray()).header("Content-Disposition",
      String.format("attachment; filename=\"contingency-table-%s-%s.csv\"", variableName, crossVariable)).build();
  }

  @GET
  @Path("/contingency/_export")
  @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @Timed
  public Response getContingencyExcel(@QueryParam("by") String crossVariable) throws IOException {
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);
    ByteArrayOutputStream res = new ExcelContingencyWriter(variables.getFirst(), variables.getSecond())
      .write(getContingencyDto(variables.getFirst(), variables.getSecond()));

    return Response.ok(res.toByteArray()).header("Content-Disposition",
      String.format("attachment; filename=\"contingency-table-%s-%s.xlsx\"", variableName, crossVariable)).build();
  }

  @Override
  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  @Override
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public void setTableType(String tableType) {
    this.tableType = tableType;
  }

  private Pair<DatasetVariable, DatasetVariable> getContingencyVariables(String crossVariable) {
    if(Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Cross variable name is required for the contingency table");

    DatasetVariable var = getDatasetVariable(datasetId, variableName, DatasetVariable.Type.Harmonized, studyId, project,
      table, tableType);
    DatasetVariable crossVar = getDatasetVariable(datasetId, crossVariable, DatasetVariable.Type.Harmonized, studyId,
      project, table, tableType);

    return Pair.create(var, crossVar);
  }

}
