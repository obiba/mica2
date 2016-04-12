package org.obiba.mica.file.rest;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.support.FileMediaType;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.io.Files;

@Component
@Path("/")
@RequiresAuthentication
public class PublishedFileSystemResource extends AbstractFileSystemResource {

  @Inject
  private FileStoreService fileStoreService;

  @Override
  protected boolean isPublishedFileSystem() {
    return true;
  }

  @GET
  @Path("/file-dl/{path:.*}")
  @Timed
  public Response downloadFile(@PathParam("path") String path,
    @QueryParam("inline") @DefaultValue("false") boolean inline) {
    Attachment attachment = doGetAttachment(path);

    if (inline) {
      String filename = attachment.getName();
      return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
        .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
        .type(FileMediaType.type(Files.getFileExtension(filename)))
        .build();
    }

    return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
      .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }

  @GET
  @Path("/file/{path:.*}")
  @Timed
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return doGetFile(path);
  }
}
