package org.obiba.mica.web.rest.file;

import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
public class TempFileResource {

  private String id;

  public void setId(String id) {
    this.id = id;
  }

//  @GET
//  @Timed
//  public FileDto getMetadata() {
//    return null;
//  }
//
//  @GET
//  @Path("/_download")
//  @Timed
//  public FileDto download() {
//    return null;
//  }

  @DELETE
  @Timed
  public Response delete() {
    //TODO delete temp file
    return Response.noContent().build();
  }
}
