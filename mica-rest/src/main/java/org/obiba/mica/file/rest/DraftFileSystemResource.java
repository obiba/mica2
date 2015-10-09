package org.obiba.mica.file.rest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileService;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/draft")
@RequiresPermissions({ "/draft:EDIT" })
public class DraftFileSystemResource {

  @Inject
  private FileSystemResourceHelper fileSystemResourceHelper;

  @Inject
  private FileService fileService;

  @PostConstruct
  public void init() {
    fileSystemResourceHelper.setPublished(false);
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

  @DELETE
  @Path("/file/{path:.*}")
  public Response deleteFile(@PathParam("path") String path) {
    fileSystemResourceHelper.deleteFile(path);
    return Response.noContent().build();
  }

  @PUT
  @Path("/file/{path:.*}")
  public Response updateFile(@PathParam("path") String path, @QueryParam("publish") Boolean publish) {
    if (publish != null) fileSystemResourceHelper.publishFile(path, publish);
    return Response.noContent().build();
  }

  @POST
  @Path("/files")
  public Response addFile(Mica.AttachmentDto attachmentDto, @Context UriInfo uriInfo) {
    fileSystemResourceHelper.addFile(attachmentDto);
    return Response.created(uriInfo.getBaseUriBuilder().path("draft").path("file").path(attachmentDto.getPath()).build())
      .build();
  }

}
