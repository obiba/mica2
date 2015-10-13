package org.obiba.mica.file.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileService;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/")
public class PublishedFileSystemResource extends AbstractFileSystemResource {

  @Inject
  private FileService fileService;

  @Override
  protected boolean isPublished() {
    return true;
  }

  @GET
  @Path("/file-dl/{path:.*}")
  public Response downloadFile(@PathParam("path") String path) {
    Attachment attachment = doGetAttachment(path);
    return Response.ok(fileService.getFile(attachment.getId()))
      .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }

  @GET
  @Path("/file/{path:.*}")
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return doGetFile(path);
  }

  @GET
  @Path("/files-search/{path:.*}")
  public List<Mica.FileDto> searchFile(@PathParam("path") String path, @QueryParam("query") String query,
    @QueryParam("recursively") @DefaultValue("false") boolean recursively) {
    return doSearchFiles(path, query, recursively);
  }
}
