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

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * Harmonized variable resource: variable of an harmonization dataset, implementing the dataschema variable with the same name.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedHarmonizedDatasetVariableResource extends AbstractPublishedDatasetResource<HarmonizationDataset>
  implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizedDatasetVariableResource.class);

  private String datasetId;

  private String variableName;

  private String studyId;

  private String project;

  private String table;

  @Inject
  private HarmonizationDatasetService datasetService;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Harmonized, studyId, project, table);
  }

  @GET
  @Path("/summary")
  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary() {
    return datasetService
      .getVariableSummary(getDataset(HarmonizationDataset.class, datasetId), variableName, studyId, project, table)
      .getWrappedDto();
  }

  @GET
  @Path("/facet")
  public Search.QueryResultDto getVariableFacet() {
    return datasetService
      .getVariableFacet(getDataset(HarmonizationDataset.class, datasetId), variableName, studyId, project, table);
  }

  @GET
  @Path("/aggregation")
  public Mica.DatasetVariableAggregationDto getVariableAggregations() {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    for(StudyTable studyTable : dataset.getStudyTables()) {
      if(studyTable.isFor(studyId, project, table)) {
        try {
          return dtos.asDto(studyTable,
            datasetService.getVariableSummary(dataset, variableName, studyId, project, table).getWrappedDto()).build();
        } catch(Exception e) {
          log.warn("Unable to retrieve statistics: " + e.getMessage(), e);
          return dtos.asDto(studyTable, null).build();
        }
      }
    }
    throw new NoSuchValueTableException(project, table);
  }

  @GET
  @Path("/contingency")
  public Mica.DatasetVariableContingencyDto getContingency(@QueryParam("by") String crossVariable) {
    if(Strings.isNullOrEmpty(crossVariable))
      throw new BadRequestException("Cross variable name is required for the contingency table");

    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);

    for(StudyTable studyTable : dataset.getStudyTables()) {
      if(studyTable.isFor(studyId, project, table)) {
        try {
          return dtos.asContingencyDto(studyTable,
            datasetService.getContingencyTable(studyTable, variableName, crossVariable)).build();
        } catch(Exception e) {
          log.warn("Unable to retrieve contingency table: " + e.getMessage(), e);
          return dtos.asContingencyDto(studyTable, null).build();
        }
      }
    }
    throw new NoSuchValueTableException(project, table);
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
}
