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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Search;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;

@Component
@Scope("request")
public class DraftStudyDatasetResource extends
  AbstractGitPersistableResource<StudyDatasetState,StudyDataset> {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @RequiresPermissions({"/draft:EDIT"})
  public Mica.DatasetDto get() {
    return dtos.asDto(getDataset());
  }

  @DELETE
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public void delete() {
    datasetService.delete(id);
  }

  @PUT
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Response update(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo) {
    if (!datasetDto.hasId() || !datasetDto.getId().equals(id)) throw new IllegalArgumentException("Not the expected dataset id");
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof StudyDataset)) throw new IllegalArgumentException("A study dataset is expected");

    datasetService.save((StudyDataset) dataset);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  @Timed
  @RequiresPermissions({"/draft:PUBLISH"})
  public Response index() {
    datasetService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  @RequiresPermissions({"/draft:PUBLISH"})
  public Response publish() {
    datasetService.publish(id, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  @RequiresPermissions({"/draft:PUBLISH"})
  public Response unPublish() {
    datasetService.publish(id, false);
    return Response.noContent().build();
  }

  @GET
  @Path("/table")
  @RequiresPermissions({"/draft:EDIT"})
  public Magma.TableDto getTable() {
    Magma.TableDto dto = datasetService.getTableDto(getDataset());
    return dto;
  }

  @GET
  @Path("/variables")
  @RequiresPermissions({"/draft:EDIT"})
  public List<Mica.DatasetVariableDto> getVariables() {
    ImmutableList.Builder<Mica.DatasetVariableDto> builder = ImmutableList.builder();
    datasetService.getDatasetVariables(getDataset()).forEach(variable -> builder.add(dtos.asDto(variable)));
    return builder.build();
  }

  @Path("/variable/{variable}")
  @RequiresPermissions({"/draft:EDIT"})
  public DraftStudyDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    DraftStudyDatasetVariableResource resource = applicationContext.getBean(DraftStudyDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @POST
  @Path("/facets")
  @RequiresPermissions({"/draft:EDIT"})
  public Search.QueryResultDto getFacets(Search.QueryTermsDto query) {
    return datasetService.getFacets(getDataset(), query);
  }

  @PUT
  @Path("/_status")
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Response toUnderReview(@QueryParam("value") String status) {
    datasetService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @GET
  @RequiresPermissions({ "/draft:EDIT" })
  @Path("/commit/{commitId}/view")
  public Mica.DatasetDto getFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    return dtos.asDto(datasetService.getFromCommit(datasetService.findDraft(id), commitId));
  }

  private StudyDataset getDataset() {
    return datasetService.findById(id);
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  protected AbstractGitPersistableService<StudyDatasetState, StudyDataset> getService() {
    return datasetService;
  }
}
