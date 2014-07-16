/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.study;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.security.Roles;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/draft")
@RequiresRoles(Roles.MICA_ADMIN)
public class DraftStudyDatasetsResource {

  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  /**
   * Get all {@link org.obiba.mica.dataset.domain.StudyDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/study-datasets")
  @Timed
  public List<Mica.DatasetDto> list(@QueryParam("study") String studyId) {
    return datasetService.findAllDatasets(studyId).stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @POST
  @Path("/study-datasets")
  @Timed
  public Response create(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo) {
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof StudyDataset)) throw new IllegalArgumentException("An study dataset is expected");

    datasetService.save((StudyDataset) dataset);
    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "study-dataset", dataset.getId()).build())
        .build();
  }

  @PUT
  @Path("/study-datasets/_index")
  @Timed
  public Response reIndex() {
    datasetService.indexAll();
    return Response.noContent().build();
  }

  @Path("/study-dataset/{id}")
  public StudyDatasetResource dataset(@PathParam("id") String id) {
    StudyDatasetResource resource = applicationContext.getBean(StudyDatasetResource.class);
    resource.setId(id);
    return resource;
  }

}
