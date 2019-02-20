package org.obiba.mica.web.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.obiba.mica.core.domain.MaximumDocumentSetCreationExceededException;

@Provider
public class MaximumDocumentSetCreationExceededExceptionMapper implements ExceptionMapper<MaximumDocumentSetCreationExceededException> {

  @Override
  public Response toResponse(MaximumDocumentSetCreationExceededException exception) {
    return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
  }
}
