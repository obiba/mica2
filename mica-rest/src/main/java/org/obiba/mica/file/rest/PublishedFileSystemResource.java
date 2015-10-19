package org.obiba.mica.file.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/")
public class PublishedFileSystemResource extends AbstractFileSystemResource {

  @Inject
  private FileStoreService fileStoreService;

  @Override
  protected boolean isPublishedFileSystem() {
    return true;
  }

  @GET
  @Path("/file-dl/{path:.*}")
  public Response downloadFile(@PathParam("path") String path) {
    Attachment attachment = doGetAttachment(path);
    return Response.ok(fileStoreService.getFile(attachment.getId()))
      .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }

  @GET
  @Path("/file/{path:.*}")
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return doGetFile(path);
  }
}
