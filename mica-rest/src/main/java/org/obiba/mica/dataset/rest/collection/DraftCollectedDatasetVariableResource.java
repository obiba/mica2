/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.collection;

import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;

@Component
@Scope("request")
public class DraftCollectedDatasetVariableResource implements DatasetVariableResource {

  private String datasetId;

  private String variableName;

  @Inject
  private CollectedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return dtos.asDto(datasetService.getDatasetVariable(getDataset(), variableName));
  }

  private StudyDataset getDataset() {
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
