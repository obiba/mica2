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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.search.rest.PublishedDatasetSearchResource;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
@Scope("request")
@Path("/harmonized-dataset/{id}")
@RequiresAuthentication
public class PublishedHarmonizedDatasetResource extends PublishedDatasetSearchResource<HarmonizedDataset> {

  @PathParam("id")
  private String id;

  @Inject
  private HarmonizedDatasetService datasetService;

  /**
   * Get {@link org.obiba.mica.dataset.domain.HarmonizedDataset} from published index.
   *
   * @return
   */
  @GET
  public Mica.DatasetDto get() {
    return getDatasetDto(HarmonizedDataset.class, id);
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable}s from published index.
   *
   * @return
   */
  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    return getDatasetVariableDtos(HarmonizedDataset.class, id);
  }

  @GET
  @Path("/variable/{name}")
  public Mica.DatasetVariableDto getVariable(@PathParam("name") String variableName) {
    return getDatasetVariableDto(HarmonizedDataset.class, id, variableName);
  }

  @GET
  @Path("/variable/{name}/summary")
  public List<org.obiba.opal.web.model.Math.SummaryStatisticsDto> getVariableSummaries(
      @PathParam("name") String variableName) {
    ImmutableList.Builder<Math.SummaryStatisticsDto> builder = ImmutableList.builder();
    HarmonizedDataset dataset = getDataset(HarmonizedDataset.class, id);
    for(StudyTable table : dataset.getStudyTables()) {
      builder.add(datasetService.getVariableSummary(dataset, variableName, table.getStudyId()));
    }
    return builder.build();
  }

  @GET
  @Path("/variable/{name}/facet")
  public List<Search.QueryResultDto> getVariableFacets(@PathParam("name") String variableName) {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizedDataset dataset = getDataset(HarmonizedDataset.class, id);
    for(StudyTable table : dataset.getStudyTables()) {
      builder.add(datasetService.getVariableFacet(dataset, variableName, table.getStudyId()));
    }
    return builder.build();
  }

  @POST
  @Path("/facets")
  public List<Search.QueryResultDto> getFacets(Search.QueryTermsDto query) {
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizedDataset dataset = getDataset(HarmonizedDataset.class, id);
    for(StudyTable table : dataset.getStudyTables()) {
      builder.add(datasetService.getFacets(dataset, query, table.getStudyId()));
    }
    return builder.build();
  }

  @Override
  protected DatasetVariable.Type getDatasetVariableType() {
    return DatasetVariable.Type.Dataschema;
  }

}
