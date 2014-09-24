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

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetResource;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * Dataschema variable resource: variable describing an harmonization dataset.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedDataschemaDatasetVariableResource
    extends AbstractPublishedDatasetResource<HarmonizationDataset> implements DatasetVariableResource {

  private String datasetId;

  private String variableName;

  @Inject
  private HarmonizationDatasetService datasetService;

  @GET
  public Mica.DatasetVariableDto getVariable() {
    return getDatasetVariableDto(datasetId, variableName, DatasetVariable.Type.Dataschema);
  }

  @GET
  @Path("/summary")
  public List<Math.SummaryStatisticsDto> getVariableSummaries() {
    ImmutableList.Builder<Math.SummaryStatisticsDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    dataset.getStudyTables().forEach(table -> {
      try {
        builder.add(datasetService.getVariableSummary(dataset, variableName, table.getStudyId()));
      } catch(NoSuchVariableException | NoSuchValueTableException e) {
        // case the study has not implemented this dataschema variable
        builder.add(Math.SummaryStatisticsDto.newBuilder().setResource(variableName).build());
      }
    });
    return builder.build();
  }

  @GET
  @Path("/facet")
  public List<Search.QueryResultDto> getVariableFacets() {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    dataset.getStudyTables().forEach(table -> {
      try {
        builder.add(datasetService.getVariableFacet(dataset, variableName, table.getStudyId()));
      } catch(NoSuchVariableException | NoSuchValueTableException e) {
        // case the study has not implemented this dataschema variable
        builder.add(Search.QueryResultDto.newBuilder().setTotalHits(0).build());
      }
    });
    return builder.build();
  }

  /**
   * Get the harmonized variable summaries for each of the study.
   *
   * @return
   */
  @GET
  @Path("/harmonizations")
  public Mica.DatasetVariableHarmonizationDto getVariableHarmonizations() {
    HarmonizationDataset dataset = getDataset(HarmonizationDataset.class, datasetId);
    return getVariableHarmonizationDto(dataset, variableName);
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
