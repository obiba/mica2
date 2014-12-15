package org.obiba.mica.dataset.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.obiba.mica.dataset.service.InvalidDatasetException;

@Provider
public class InvalidDatasetExceptionMapper implements ExceptionMapper<InvalidDatasetException> {
  @Override
  public Response toResponse(InvalidDatasetException e) {
    return Response.status(Status.CONFLICT).entity(e.getMessage()).build();
  }
}
