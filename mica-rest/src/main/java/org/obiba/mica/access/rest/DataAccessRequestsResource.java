package org.obiba.mica.access.rest;

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
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
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

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Timed
  public List<Mica.DataAccessRequestDto> list(@QueryParam("applicant") String applicant) {
    String applicantFilter = applicant;
    Subject caller = SecurityUtils.getSubject();
    if(!caller.hasRole(Roles.MICA_ADMIN) && !caller.hasRole(Roles.MICA_DAO)) {
      applicantFilter = caller.getPrincipal().toString();
    }
    return dataAccessRequestService.findAll(applicantFilter).stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @POST
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessRequest request = dtos.fromDto(dto);

    // force applicant and make sure it is a new request
    String applicant = SecurityUtils.getSubject().getPrincipal().toString();
    request.setApplicant(applicant);
    request.setId(null);
    request.setStatus(DataAccessRequest.Status.DRAFT);

    dataAccessRequestService.save(request);

    subjectAclService.addUserPermission(applicant, "/data-access-request", "EDIT", request.getId());

    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", request.getId()).build()).build();
  }

}
