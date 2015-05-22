package org.obiba.mica.access.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@Path("/data-access-request/{id}")
public class DataAccessRequestResource {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private Dtos dtos;

  @GET
  @Timed
  public Mica.DataAccessRequestDto get(@PathParam("id") String id) {
    checkPermission(id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    return dtos.asDto(request);
  }

  @DELETE
  public Response delete(@PathParam("id") String id) {
    checkPermission(id);
    try {
      dataAccessRequestService.delete(id);
    } catch(NoSuchDataAccessRequestException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  private void checkPermission(String id) {
    SecurityUtils.getSubject().checkPermission("/data-access-request:EDIT:" + id);
  }
}
