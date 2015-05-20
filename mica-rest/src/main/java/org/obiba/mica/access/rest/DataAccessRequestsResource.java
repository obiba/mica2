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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.core.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/")
@RequiresAuthentication
public class DataAccessRequestsResource {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/data-access-requests")
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
  @Path("/data-access-requests")
  @Timed
  public Response create(Mica.DataAccessRequestDto dto, @Context UriInfo uriInfo) {
    DataAccessRequest request = dtos.fromDto(dto);

    // force applicant and make sure it is a new request
    request.setApplicant(SecurityUtils.getSubject().getPrincipal().toString());
    request.setId(null);
    request.setStatus(DataAccessRequest.Status.DRAFT);

    dataAccessRequestService.save(request);
    return Response.created(uriInfo.getBaseUriBuilder().segment("data-access-request", request.getId()).build()).build();
  }

  @Path("/data-access-request/{id}")
  public DataAccessRequestResource dataset(@PathParam("id") String id) {
    DataAccessRequestResource resource = applicationContext.getBean(DataAccessRequestResource.class);
    resource.setId(id);
    return resource;
  }

}
