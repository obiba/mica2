package org.obiba.mica.access.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.core.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
public class DataAccessRequestResource {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.DataAccessRequestDto get() {
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if(isAuthorized(request)) return dtos.asDto(request);

    throw new ForbiddenException();
  }

  @DELETE
  public Response delete() {
    try {
      if(isAuthorized(dataAccessRequestService.findById(id))) dataAccessRequestService.delete(id);
    } catch(NoSuchDataAccessRequestException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  // TODO replace by ad-hoc permissions
  private boolean isAuthorized(DataAccessRequest request) {
    Subject caller = SecurityUtils.getSubject();
    return caller.hasRole(Roles.MICA_ADMIN) || caller.hasRole(Roles.MICA_DAO) ||
      request.getApplicant().equals(caller.getPrincipal().toString());
  }
}
