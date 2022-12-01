/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.harmonization;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class DraftHarmonizedDatasetVariableResource implements DatasetVariableResource {

  private String datasetId;

  private String variableName;

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  private String studyId;

  private String source;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return dtos.asDto(datasetService.getDatasetVariable(getDataset(), variableName, studyId, source));
  }

  @GET
  @Path("/summary")
  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary() {
    return datasetService.getVariableSummary(getDataset(), variableName, studyId, source).getWrappedDto();
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  public void setSource(String source) {
    this.source = source;
  }

  private HarmonizationDataset getDataset() {
    return datasetService.findById(datasetId);
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
