package org.obiba.mica.access.rest;

import com.codahale.metrics.annotation.Timed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessAmendmentsResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  private String parentId;

  @POST
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessAmendment amendment = dtos.fromAmendmentDto(dto);
    return saveNew(amendment, uriInfo);
  }

  @POST
  @Path("/_empty")
  public Response create(@Context UriInfo uriInfo) {
    DataAccessAmendment amendment = new DataAccessAmendment();
    amendment.setContent("{}");
    return saveNew(amendment, uriInfo);
  }

  private Response saveNew(DataAccessAmendment amendment, UriInfo uriInfo) {
    String resource = String.format("/data-access-request/%s/amendment", parentId);
    subjectAclService.checkPermission(resource, "ADD");
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    // force applicant and make sure it is a new request
    amendment.setApplicant(request.getApplicant());
    amendment.setId(null);
    amendment.setParentId(parentId);
    amendment.setStatus(DataAccessEntityStatus.OPENED);

    // set permissions
    dataAccessAmendmentService.save(amendment);
    resource = String.format("/data-access-request/%s/amendment", parentId);
    subjectAclService.addUserPermission(amendment.getApplicant(), resource, "VIEW,EDIT,DELETE", amendment.getId());
    subjectAclService.addUserPermission(amendment.getApplicant(), resource + "/" + amendment.getId(), "EDIT", "_status");

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", parentId, "amendment", amendment.getId()).build()).build();
  }

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    return listByStatusFilteringPermitted(status).stream()
      .map(dtos::asAmendmentDto)
      .collect(Collectors.toList());
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @GET
  @Path("/_history")
  public List<StatusChangeDto> getLoggedHistory() {
    List<StatusChangeDto> statusChangeDtos = new ArrayList<>();
    dataAccessAmendmentService.findByParentId(parentId).forEach(amendment ->
      statusChangeDtos.addAll(dtos.asStatusChangeDtoList(amendment))
    );

    return statusChangeDtos;
  }

  private List<DataAccessAmendment> listByStatusFilteringPermitted(List<String> status) {
    String resource = "/data-access-request";
    List<DataAccessAmendment> amendments = dataAccessAmendmentService.findByStatus(parentId, status);
    return amendments.stream() //
      .filter(amendment -> subjectAclService.isPermitted(resource, "VIEW", parentId)) //
      .collect(Collectors.toList());
  }
}
