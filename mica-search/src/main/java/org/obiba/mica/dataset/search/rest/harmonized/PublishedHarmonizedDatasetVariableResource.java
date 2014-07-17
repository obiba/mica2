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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetVariableResource;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Search;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Harmonized variable resource: variable of an harmonized dataset, implementing the dataschema variable with the same name.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedHarmonizedDatasetVariableResource
    extends AbstractPublishedDatasetVariableResource<HarmonizedDataset> {

  @PathParam("id")
  private String datasetId;

  @PathParam("name")
  private String variableName;

  @PathParam("id")
  private String studyId;

  @Inject
  private HarmonizedDatasetService datasetService;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(HarmonizedDataset.class, datasetId, variableName, studyId);
  }

  @GET
  @Path("/summary")
  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary() {
    return datasetService.getVariableSummary(getDataset(HarmonizedDataset.class, datasetId), variableName, studyId);
  }

  @GET
  @Path("/facet")
  public Search.QueryResultDto getVariableFacet() {
    return datasetService.getVariableFacet(getDataset(HarmonizedDataset.class, datasetId), variableName, studyId);
  }

  @Override
  protected DatasetVariable.Type getDatasetVariableType() {
    return DatasetVariable.Type.Harmonized;
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
}
