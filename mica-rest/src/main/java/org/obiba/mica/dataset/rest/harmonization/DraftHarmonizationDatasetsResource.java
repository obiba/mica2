/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.harmonization;

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

import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/draft")
public class DraftHarmonizationDatasetsResource {

  @Inject
  private HarmonizationDatasetService datasetService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private EventBus eventBus;

  /**
   * Get all {@link HarmonizationDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/harmonization-datasets")
  @Timed
  public List<Mica.DatasetDto> list(@QueryParam("study") String studyId) {
    return datasetService.findAllDatasets(studyId).stream()
      .filter(s -> subjectAclService.isPermitted("/draft/harmonization-dataset", "VIEW", s.getId()))
      .map(d -> dtos.asDto(d, true)).collect(Collectors.toList());
  }

  @POST
  @Path("/harmonization-datasets")
  @Timed
  @RequiresPermissions({ "/draft/harmonization-dataset:ADD" })
  public Response create(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo) {
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof HarmonizationDataset))
      throw new IllegalArgumentException("An harmonization dataset is expected");

    datasetService.save((HarmonizationDataset) dataset);
    return Response
      .created(uriInfo.getBaseUriBuilder().segment("draft", "harmonization-dataset", dataset.getId()).build()).build();
  }

  @PUT
  @Path("/harmonization-datasets/_index")
  @Timed
  @RequiresPermissions({ "/draft/harmonization-dataset:PUBLISH" })
  public Response reIndex() {
    eventBus.post(new IndexDatasetsEvent());
    return Response.noContent().build();
  }

  @Path("/harmonization-dataset/{id}")
  public DraftHarmonizationDatasetResource dataset(@PathParam("id") String id) {
    DraftHarmonizationDatasetResource resource = applicationContext.getBean(DraftHarmonizationDatasetResource.class);
    resource.setId(id);
    return resource;
  }

}
