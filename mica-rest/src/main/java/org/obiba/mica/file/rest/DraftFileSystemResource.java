/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.rest;


import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.file.support.FileMediaType;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Component
@Path("/draft")
public class DraftFileSystemResource extends AbstractFileSystemResource {

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private TempFileService tempFileService;

  @Override
  protected boolean isPublishedFileSystem() {
    return false;
  }

  @GET
  @Path("/file-dl/{path:.*}")
  public Response downloadFile(@PathParam("path") String path, @QueryParam("version") String version,
    @QueryParam("inline") @DefaultValue("false") boolean inline, @QueryParam("key") String shareKey) {

    try {
      Attachment attachment = doGetAttachment(path, version, shareKey);

      if (inline) {
        String filename = attachment.getName();
        return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
          .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
          .type(FileMediaType.type(Files.getFileExtension(filename)))
          .build();
      }

      return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
        .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
    } catch (NoSuchEntityException e) {
      String name = doZip(path);

      return Response.ok(tempFileService.getInputStreamFromFile(name))
        .header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
    }
  }

  @GET
  @Path("/file/{path:.*}")
  public Mica.FileDto getFile(@PathParam("path") String path, @QueryParam("key") String shareKey) {
    return doGetFile(path, shareKey);
  }

  @DELETE
  @Path("/file/{path:.*}")
  public Response deleteFile(@PathParam("path") String path) {
    doDeleteFile(path);
    return Response.noContent().build();
  }

  @PUT
  @Path("/file/{path:.*}")
  public Response updateFile(@PathParam("path") String path, @QueryParam("status") String status,
    @QueryParam("publish") Boolean publish, @QueryParam("name") String newName, @QueryParam("move") String movePath,
    @QueryParam("copy") String copyPath, @QueryParam("version") String versionId) {

    if(!Strings.isNullOrEmpty(versionId)) {
      doReinstate(path, versionId);
    } else if(!Strings.isNullOrEmpty(copyPath)) {
      if(!Strings.isNullOrEmpty(movePath))
        throw new IllegalArgumentException("Copy and move are mutually exclusive operations");
      if(!Strings.isNullOrEmpty(newName))
        throw new IllegalArgumentException("Copy and rename are mutually exclusive operations");
      doCopyFile(path, copyPath);
    } else if(!Strings.isNullOrEmpty(movePath)) {
      if(!Strings.isNullOrEmpty(newName))
        throw new IllegalArgumentException("Move and rename are mutually exclusive operations");
      doMoveFile(path, movePath);
    } else if(!Strings.isNullOrEmpty(newName)) doRenameFile(path, newName);

    if(publish != null) doPublishFile(path, publish);

    if (!Strings.isNullOrEmpty(status)) doUpdateStatus(path, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @POST
  @Path("/files")
  public Response addFile(Mica.AttachmentDto attachmentDto, @Context UriInfo uriInfo) {
    doAddOrUpdateFile(attachmentDto);
    return Response
      .created(uriInfo.getBaseUriBuilder().path("draft").path("file").path(attachmentDto.getPath()).build()).build();
  }

  private void doReinstate(String path, String versionId) {
    Attachment attachment = doGetAttachment(path, versionId);
    fileSystemService.reinstate(attachment);
  }
}
