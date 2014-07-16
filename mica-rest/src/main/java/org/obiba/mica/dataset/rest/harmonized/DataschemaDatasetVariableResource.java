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

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.rest.variable.DatasetVariableResource;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
@Scope("request")
@RequiresAuthentication
public class DataschemaDatasetVariableResource extends DatasetVariableResource {

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return dtos.asDto(datasetService.getDatasetVariable(getDataset(), getName()));
  }

  @GET
  @Path("/summary")
  public List<Math.SummaryStatisticsDto> getVariableSummaries() {
    ImmutableList.Builder<Math.SummaryStatisticsDto> builder = ImmutableList.builder();
    for (StudyTable table : getDataset().getStudyTables()) {
      builder.add(datasetService.getVariableSummary(getDataset(), getName(), table.getStudyId()));
    }
    return builder.build();
  }

  @GET
  @Path("/facet")
  public List<Search.QueryResultDto> getVariableFacet() {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    for (StudyTable table : getDataset().getStudyTables()) {
      builder.add(datasetService.getVariableFacet(getDataset(), getName(), table.getStudyId()));
    }
    return builder.build();
  }

  private HarmonizedDataset getDataset() {
    return datasetService.findById(getDatasetId());
  }

}
