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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetVariableResource;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * Dataschema variable resource: variable describing an harmonized dataset.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedDataschemaDatasetVariableResource
    extends AbstractPublishedDatasetVariableResource<HarmonizedDataset> {

  @PathParam("id")
  private String datasetId;

  @PathParam("name")
  private String variableName;

  @Inject
  private HarmonizedDatasetService datasetService;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(HarmonizedDataset.class, datasetId, variableName);
  }

  @GET
  @Path("/summary")
  public List<Math.SummaryStatisticsDto> getVariableSummaries() {
    ImmutableList.Builder<Math.SummaryStatisticsDto> builder = ImmutableList.builder();
    HarmonizedDataset dataset = getDataset(HarmonizedDataset.class, datasetId);
    for(StudyTable table : dataset.getStudyTables()) {
      builder.add(datasetService.getVariableSummary(dataset, variableName, table.getStudyId()));
    }
    return builder.build();
  }

  @GET
  @Path("/facet")
  public List<Search.QueryResultDto> getVariableFacets() {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizedDataset dataset = getDataset(HarmonizedDataset.class, datasetId);
    for(StudyTable table : dataset.getStudyTables()) {
      builder.add(datasetService.getVariableFacet(dataset, variableName, table.getStudyId()));
    }
    return builder.build();
  }

  @Override
  protected DatasetVariable.Type getDatasetVariableType(String studyId) {
    return DatasetVariable.Type.Dataschema;
  }

  @Override
  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  @Override
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }
}
