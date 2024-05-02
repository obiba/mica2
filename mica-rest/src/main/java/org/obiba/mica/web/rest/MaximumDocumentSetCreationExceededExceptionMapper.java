package org.obiba.mica.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.obiba.mica.core.domain.MaximumDocumentSetCreationExceededException;

@Provider
public class MaximumDocumentSetCreationExceededExceptionMapper implements ExceptionMapper<MaximumDocumentSetCreationExceededException> {

  @Override
  public Response toResponse(MaximumDocumentSetCreationExceededException exception) {
    return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
  }
}
