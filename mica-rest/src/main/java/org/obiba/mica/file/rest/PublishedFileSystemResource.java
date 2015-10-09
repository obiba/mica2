package org.obiba.mica.file.rest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/")
public class PublishedFileSystemResource {

  @Inject
  private FileSystemResourceHelper fileSystemResourceHelper;

  @PostConstruct
  public void init() {
    fileSystemResourceHelper.setPublished(true);
  }

  @GET
  @Path("/files/{path:.*}")
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return fileSystemResourceHelper.getFile(path);
  }
}
