package org.obiba.mica.web.rest.file;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class FileResource {

  private String repo;

  private String id;

  public void setRepo(String repo) {
    this.repo = repo;
  }

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
}
