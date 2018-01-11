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

import com.google.common.collect.ImmutableList;
import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Search;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Scope("request")
public class DraftCollectedDatasetResource extends
  AbstractGitPersistableResource<StudyDatasetState,StudyDataset> {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private CollectedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  public Mica.DatasetDto get(@QueryParam("key") String key) {
    checkPermission("/draft/collected-dataset", "VIEW", key);
    return dtos.asDto(getDataset(), true);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    checkPermission("/draft/collected-dataset", "VIEW");
    return datasetService.findById(id).getModel();
  }

  @DELETE
  public void delete() {
    checkPermission("/draft/collected-dataset", "DELETE");
    datasetService.delete(id);
  }

  @PUT
  public Response update(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    checkPermission("/draft/collected-dataset", "EDIT");
    if (!datasetDto.hasId() || !datasetDto.getId().equals(id)) throw new IllegalArgumentException("Not the expected dataset id");
    Dataset dataset = dtos.fromDto(datasetDto);
    if(!(dataset instanceof StudyDataset)) throw new IllegalArgumentException("A study dataset is expected");

    datasetService.save((StudyDataset) dataset, comment);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  public Response index() {
    checkPermission("/draft/collected-dataset", "EDIT");
    datasetService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    checkPermission("/draft/collected-dataset", "PUBLISH");
    datasetService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    checkPermission("/draft/collected-dataset", "PUBLISH");
    datasetService.publish(id, false);
    return Response.noContent().build();
  }

  @GET
  @Path("/table")
  public Magma.TableDto getTable() {
    checkPermission("/draft/collected-dataset", "VIEW");
    return datasetService.getTableDto(getDataset());
  }

  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    checkPermission("/draft/collected-dataset", "VIEW");
    ImmutableList.Builder<Mica.DatasetVariableDto> builder = ImmutableList.builder();
    datasetService.getDatasetVariables(getDataset()).forEach(variable -> builder.add(dtos.asDto(variable)));
    return builder.build();
  }

  @Path("/variable/{variable}")
  public DraftCollectedDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    checkPermission("/draft/collected-dataset", "VIEW");
    DraftCollectedDatasetVariableResource resource = applicationContext.getBean(DraftCollectedDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @POST
  @Path("/facets")
  public Search.QueryResultDto getFacets(Search.QueryTermsDto query) {
    checkPermission("/draft/collected-dataset", "VIEW");
    return datasetService.getFacets(getDataset(), query);
  }

  @PUT
  @Path("/_status")
  public Response updateStatus(@QueryParam("value") String status) {
    checkPermission("/draft/collected-dataset", "VIEW");
    datasetService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.DatasetDto getFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/collected-dataset", "VIEW");
    return dtos.asDto(datasetService.getFromCommit(datasetService.findDraft(id), commitId), true);
  }

  @Path("/permissions")
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/collected-dataset", id);
    subjectAclResource.setFileResourceInstance("/draft/file", "/collected-dataset/" + id);
    return subjectAclResource;
  }

  @Path("/accesses")
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/collected-dataset", id);
    subjectAclResource.setFileResourceInstance("/file", "/collected-dataset/" + id);
    return subjectAclResource;
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
