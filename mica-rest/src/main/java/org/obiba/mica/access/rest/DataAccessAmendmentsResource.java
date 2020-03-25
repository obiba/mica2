package org.obiba.mica.access.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessAmendmentsResource {

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

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

    // force applicant and make sure it is a new request
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    amendment.setApplicant(applicant);
    amendment.setId(null);
    amendment.setParentId(parentId);
    amendment.setStatus(DataAccessEntityStatus.OPENED);

    dataAccessAmendmentService.save(amendment);
    resource = String.format("/data-access-request/%s/amendment", parentId);

    subjectAclService.addPermission(resource, "VIEW,EDIT,DELETE", amendment.getId());
    subjectAclService.addPermission(resource + "/" + amendment.getId(), "EDIT", "_status");

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
