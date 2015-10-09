package org.obiba.mica.file.rest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileService;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/")
public class PublishedFileSystemResource {

  @Inject
  private FileSystemResourceHelper fileSystemResourceHelper;

  @Inject
  private FileService fileService;

  @PostConstruct
  public void init() {
    fileSystemResourceHelper.setPublished(true);
  }

  @GET
  @Path("/file-dl/{path:.*}")
  public Response downloadFile(@PathParam("path") String path) {
    Attachment attachment = fileSystemResourceHelper.getAttachment(path);
    return Response.ok(fileService.getFile(attachment.getId()))
      .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }

  @GET
  @Path("/file/{path:.*}")
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return fileSystemResourceHelper.getFile(path);
  }
}
