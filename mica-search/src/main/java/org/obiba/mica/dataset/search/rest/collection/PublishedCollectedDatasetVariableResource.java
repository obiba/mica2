/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.collection;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.commons.math3.util.Pair;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.search.rest.harmonization.CsvContingencyWriter;
import org.obiba.mica.dataset.search.rest.harmonization.ExcelContingencyWriter;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Scope("request")
public class PublishedCollectedDatasetVariableResource extends AbstractPublishedDatasetResource<StudyDataset>
  implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedCollectedDatasetVariableResource.class);

  private String datasetId;

  private String variableName;

  @Inject
  private CollectedDatasetService datasetService;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    checkDatasetAccess();
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Collected);
  }

  @GET
  @Path("/aggregation")
  @Timed
  public Mica.DatasetVariableAggregationDto getVariableAggregations(@QueryParam("study") @DefaultValue("true") boolean withStudySummary) {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    StudyDataset dataset = getDataset(StudyDataset.class, datasetId);
    StudyTable studyTable = dataset.getSafeStudyTable();
    try {
      Mica.DatasetVariableAggregationDto summary = datasetService.getVariableSummary(dataset, variableName);
      return dtos.asDto(studyTable, datasetService.getFilteredVariableSummary(summary), withStudySummary).build();
    } catch (Exception e) {
      if (log.isDebugEnabled())
        log.warn("Unable to retrieve statistics: {}", e.getMessage(), e);
      else
        log.warn("Unable to retrieve statistics: {}", e.getMessage());
      return dtos.asDto(studyTable, null, withStudySummary).build();
    }
  }

  @GET
  @Path("/contingency")
  @Timed
  public Mica.DatasetVariableContingencyDto getContingency(@QueryParam("by") String crossVariable) {
    checkDatasetAccess();
    checkVariableSummaryAccess();
    checkContingencyAccess();
    Pair<DatasetVariable, DatasetVariable> variables = getContingencyVariables(crossVariable);
    return getContingencyDto(variables.getFirst(), variables.getSecond());
  }

  private Mica.DatasetVariableContingencyDto getContingencyDto(DatasetVariable var, DatasetVariable crossVar) {
    StudyDataset dataset = getDataset(StudyDataset.class, datasetId);
    StudyTable studyTable = dataset.getSafeStudyTable();

    try {
      return dtos
        .asContingencyDto(studyTable, datasetService.getContingencyTable(dataset, var, crossVar))
        .build();
    } catch (Exception e) {
      log.warn("Unable to retrieve contingency table: " + e.getMessage(), e);
      return dtos.asContingencyDto(studyTable, null).build();
    }
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
    checkDatasetAccess();
    checkVariableSummaryAccess();
    checkContingencyAccess();
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

  private Pair<DatasetVariable, DatasetVariable> getContingencyVariables(String crossVariable) {
    if (Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Cross variable name is required for the contingency table");

    DatasetVariable var = getDatasetVariable(datasetId, variableName, DatasetVariable.Type.Collected, null);
    DatasetVariable crossVar = getDatasetVariable(datasetId, crossVariable, DatasetVariable.Type.Collected, null);

    return Pair.create(var, crossVar);
  }

  private void checkDatasetAccess() {
    subjectAclService.checkAccess("/collected-dataset", datasetId);
  }
}
