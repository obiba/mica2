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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapDifference;

import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.study.service.DocumentDifferenceService;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
  @Path("/table")
  public Magma.TableDto getTable() {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    return datasetService.getTableDto(getDataset());
  }

  @GET
  @Path("/variables")
  public List<Mica.DatasetVariableDto> getVariables() {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    ImmutableList.Builder<Mica.DatasetVariableDto> builder = ImmutableList.builder();
    datasetService.getDatasetVariables(getDataset()).forEach(variable -> builder.add(dtos.asDto(variable)));
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

  @POST
  @Path("/facets")
  public List<Search.QueryResultDto> getFacets(Search.QueryTermsDto query) {
    checkPermission("/draft/harmonized-dataset", "VIEW");
    ImmutableList.Builder<Search.QueryResultDto> builder = ImmutableList.builder();
    HarmonizationDataset dataset = getDataset();
    for (BaseStudyTable table : dataset.getBaseStudyTables()) {
      builder.add(datasetService.getFacets(query, table));
    }
    return builder.build();
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

  @GET
  @Path("/diff")
  public Response diff(@NotNull @QueryParam("left") String left, @NotNull @QueryParam("right") String right) {
    checkPermission("/draft/individual-study", "VIEW");

    HarmonizationDataset leftCommit = datasetService.getFromCommit(datasetService.findDraft(id), left);
    HarmonizationDataset rightCommit = datasetService.getFromCommit(datasetService.findDraft(id), right);

    Map<String, Object> data = new HashMap<>();

    try {
      MapDifference<String, Object> difference = DocumentDifferenceService.diff(leftCommit, rightCommit);

      data.put("differing", DocumentDifferenceService.fromEntriesDifferenceMap(difference.entriesDiffering()));
      data.put("onlyLeft", difference.entriesOnlyOnLeft());
      data.put("onlyRight", difference.entriesOnlyOnRight());
      data.put("inCommon", difference.entriesInCommon());

    } catch (JsonProcessingException e) {
      //
    }

    return Response.ok(data, MediaType.APPLICATION_JSON_TYPE).build();
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
