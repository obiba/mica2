package org.obiba.mica.dataset.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.obiba.mica.dataset.service.DatasourceNotAvailableException;

@Provider
public class DatasourceNotAvailableExceptionMapper implements ExceptionMapper<DatasourceNotAvailableException> {
  @Override
  public Response toResponse(DatasourceNotAvailableException e) {
    return Response.status(Status.SERVICE_UNAVAILABLE).entity("Verify the datasource is available.").build();
  }
}
