package org.obiba.mica.study.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.obiba.mica.study.ConstraintException;

@Provider
public class ConstraintExceptionMapper implements ExceptionMapper<ConstraintException> {
  @Override
  public Response toResponse(ConstraintException e) {
    return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON_TYPE)
      .entity(e.getConflicts()).build();
  }
}
