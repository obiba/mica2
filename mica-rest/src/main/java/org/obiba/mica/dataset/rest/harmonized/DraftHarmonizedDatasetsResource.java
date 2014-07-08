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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/draft")
@RequiresAuthentication
public class DraftHarmonizedDatasetsResource {

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  /**
   * Get all {@link org.obiba.mica.dataset.domain.HarmonizedDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/harmonized-datasets")
  public List<Mica.DatasetDto> getDatasets(@QueryParam("study") String studyId) {
    return datasetService.findAllDatasets(studyId).stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @Path("/harmonized-dataset/{id}")
  public HarmonizedDatasetResource getDataset(@PathParam("id") String id) {
    HarmonizedDatasetResource resource = applicationContext.getBean(HarmonizedDatasetResource.class);
    resource.setId(id);
    return resource;
  }

  @PUT
  @Path("/harmonized-dataset/{id}/_publish")
  public Response publishDataset(@PathParam("id") String id) {
    datasetService.publish(id, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/harmonized-dataset/{id}/_publish")
  public Response unPublishDataset(@PathParam("id") String id) {
    datasetService.publish(id, false);
    return Response.noContent().build();
  }

}
