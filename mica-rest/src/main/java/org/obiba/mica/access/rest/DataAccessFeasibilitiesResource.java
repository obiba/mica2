package org.obiba.mica.access.rest;

import com.codahale.metrics.annotation.Timed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessFeasibilityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessFeasibilitiesResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  private String parentId;

  @POST
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessFeasibility feasibility = dtos.fromFeasibilityDto(dto);
    return saveNew(feasibility, uriInfo);
  }

  @POST
  @Path("/_empty")
  public Response create(@Context UriInfo uriInfo) {
    DataAccessFeasibility feasibility = new DataAccessFeasibility();
    feasibility.setContent("{}");
    return saveNew(feasibility, uriInfo);
  }

  private Response saveNew(DataAccessFeasibility feasibility, UriInfo uriInfo) {
    String resource = String.format("/data-access-request/%s/feasibilities", parentId);
    subjectAclService.checkPermission(resource, "ADD");
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    // force applicant and make sure it is a new request
    feasibility.setApplicant(request.getApplicant());
    feasibility.setId(null);
    feasibility.setParentId(parentId);
    feasibility.setStatus(DataAccessEntityStatus.OPENED);

    // set permissions
    dataAccessFeasibilityService.save(feasibility);
    resource = String.format("/data-access-request/%s/feasibility", parentId);
    subjectAclService.addUserPermission(feasibility.getApplicant(), resource, "VIEW,EDIT,DELETE", feasibility.getId());
    subjectAclService.addUserPermission(feasibility.getApplicant(), resource + "/" + feasibility.getId(), "EDIT", "_status");

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", parentId, "feasibility", feasibility.getId()).build()).build();
  }

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    return listByStatusFilteringPermitted(status).stream()
      .map(dtos::asFeasibilityDto)
      .collect(Collectors.toList());
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @GET
  @Path("/_history")
  public List<StatusChangeDto> getLoggedHistory() {
    List<StatusChangeDto> statusChangeDtos = new ArrayList<>();
    dataAccessFeasibilityService.findByParentId(parentId).forEach(feasibility ->
      statusChangeDtos.addAll(dtos.asStatusChangeDtoList(feasibility))
    );

    return statusChangeDtos;
  }

  private List<DataAccessFeasibility> listByStatusFilteringPermitted(List<String> status) {
    String resource = "/data-access-request";
    List<DataAccessFeasibility> feasibilities = dataAccessFeasibilityService.findByStatus(parentId, status);
    return feasibilities.stream() //
      .filter(feasibility -> subjectAclService.isPermitted(resource, "VIEW", parentId)) //
      .collect(Collectors.toList());
  }
}
