/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.harmonization;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("request")
public class DraftHarmonizedDatasetResource extends
  AbstractGitPersistableResource<HarmonizationDatasetState, HarmonizationDataset> {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private HarmonizedDatasetService datasetService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  public Mica.DatasetDto get(@QueryParam("key") String key) {
    checkPermission("/draft/harmonized-dataset", "VIEW", key);
    return dtos.asDto(getDataset(), true);
  }

  @GET
  @Path("/model")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getModel() {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    return getDataset().getModel();
  }

  @PUT
  @Path("/model")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateModel(String body) {
    checkPermission("/draft/harmonized-dataset", "EDIT");
    HarmonizationDataset dataset = getDataset();
    dataset.setModel(Strings.isNullOrEmpty(body) ? new HashMap<>() : JSONUtils.toMap(body));
    datasetService.save(dataset);
    return Response.ok().build();
  }

  @DELETE
  public void delete() {
    checkPermission("/draft/harmonized-dataset", "DELETE");
    datasetService.delete(id);
  }

  @PUT
  @Timed
  public Response update(Mica.DatasetDto datasetDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    checkPermission("/draft/harmonized-dataset", "EDIT");
    if (!datasetDto.hasId() || !datasetDto.getId().equals(id))
      throw new IllegalArgumentException("Not the expected dataset id");
    Dataset dataset = dtos.fromDto(datasetDto);
    if (!(dataset instanceof HarmonizationDataset))
      throw new IllegalArgumentException("An harmonization dataset is expected");

    datasetService.save((HarmonizationDataset) dataset, comment);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  @Timed
  public Response index() {
    checkPermission("/draft/harmonized-dataset", "EDIT");
    datasetService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    checkPermission("/draft/harmonized-dataset", "PUBLISH");
    datasetService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    checkPermission("/draft/harmonized-dataset", "PUBLISH");
    datasetService.publish(id, false);
    return Response.noContent().build();
  }

  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    ImmutableList.Builder<Mica.DatasetVariableDto> builder = ImmutableList.builder();
    datasetService.getDatasetVariables(getDataset(), null).forEach(variable -> builder.add(dtos.asDto(variable)));
    return builder.build();
  }

  @Path("/variable/{variable}")
  public DraftDataschemaDatasetVariableResource getVariable(@PathParam("variable") String variable) {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    DraftDataschemaDatasetVariableResource resource = applicationContext.getBean(DraftDataschemaDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    return resource;
  }

  @Path("/study/{study}/variable/{variable}")
  public DraftHarmonizedDatasetVariableResource getVariable(@PathParam("study") String studyId, @PathParam("variable") String variable) {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    DraftHarmonizedDatasetVariableResource resource = applicationContext.getBean(DraftHarmonizedDatasetVariableResource.class);
    resource.setDatasetId(id);
    resource.setVariableName(variable);
    resource.setStudyId(studyId);
    return resource;
  }

  @PUT
  @Path("/_status")
  @Timed
  public Response updateStatus(@QueryParam("value") String status) {
    checkPermission("/draft/harmonized-dataset", "EDIT");
    datasetService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.DatasetDto getFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    return dtos.asDto(datasetService.getFromCommit(datasetService.findDraft(id), commitId), true);
  }

  @Path("/permissions")
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/harmonized-dataset", id);
    subjectAclResource.setFileResourceInstance("/draft/file", "/harmonized-dataset/" + id);
    return subjectAclResource;
  }

  @Path("/accesses")
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/harmonized-dataset", id);
    subjectAclResource.setFileResourceInstance("/file", "/harmonized-dataset/" + id);
    return subjectAclResource;
  }

  private HarmonizationDataset getDataset() {
    return datasetService.findById(id);
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  protected AbstractGitPersistableService<HarmonizationDatasetState, HarmonizationDataset> getService() {
    return datasetService;
  }
}
