/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.harmonized;

import javax.inject.Inject;
import javax.ws.rs.GET;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.rest.variable.DatasetVariableResource;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@RequiresAuthentication
public class HarmonizedDatasetVariableResource extends DatasetVariableResource {

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  private String studyId;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return dtos.asDto(datasetService.getDatasetVariable(getDataset(), getName(), studyId));
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  private HarmonizedDataset getDataset() {
    return datasetService.findById(getDatasetId());
  }

}
