package org.obiba.mica.access.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/data-access-requests")
public class DataAccessRequestsResource {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private Dtos dtos;

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> listByStatus(@QueryParam("status") List<String> status) {
    List<DataAccessRequest> reqs = dataAccessRequestService.findByStatus(status);
    return reqs.stream() //
      .filter(req -> subjectAclService.isPermitted("/data-access-request", "VIEW", req.getId())) //
      .map(dtos::asDto).collect(Collectors.toList());
  }

  @GET
  @Path("/applicant/{applicant}")
  @Timed
  public List<Mica.DataAccessRequestDto> list(@PathParam("applicant") String applicant) {
    return dataAccessRequestService.findAll(applicant).stream() //
      .filter(req -> subjectAclService.isPermitted("/data-access-request", "VIEW", req.getId())) //
      .map(dtos::asDto).collect(Collectors.toList());
  }

  @POST
  @Timed
  @RequiresPermissions("/data-access-request:ADD")
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessRequest request = dtos.fromDto(dto);

    // force applicant and make sure it is a new request
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    request.setApplicant(applicant);
    request.setId(null);
    request.setStatus(DataAccessRequest.Status.OPENED);

    dataAccessRequestService.save(request);

    subjectAclService.addPermission("/data-access-request", "VIEW,EDIT,DELETE", request.getId());
    subjectAclService.addPermission("/data-access-request/" + request.getId(), "EDIT", "_status");
    subjectAclService.addPermission("/data-access-request/" + request.getId() + "/comments", "ADD");
    subjectAclService.addGroupPermission(Roles.MICA_DAO, "/data-access-request/" + request.getId() + "/comments", "ADD", null);

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", request.getId()).build()).build();
  }

}
