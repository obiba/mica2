package org.obiba.mica.micaConfig.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.obiba.mica.micaConfig.NoSuchOpalCredential;

@Provider
public class NoSuchOpalCredentialExceptionMapper implements ExceptionMapper<NoSuchOpalCredential> {

  @Override
  public Response toResponse(NoSuchOpalCredential noSuchOpalCredentialFound) {
    return Response.status(Response.Status.NOT_FOUND).build();
  }
}
