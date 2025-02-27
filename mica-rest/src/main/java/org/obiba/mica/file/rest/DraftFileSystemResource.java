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


import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.file.support.FileMediaType;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Strings;
import com.google.common.io.Files;

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
      String filename = attachment.getName();
      String uriEncodedFilename = UriUtils.encode(filename, "UTF-8");

      if (inline) {
        return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
          .header("Content-Disposition", "inline; filename=\"" + uriEncodedFilename + "\"")
          .type(FileMediaType.type(Files.getFileExtension(filename)))
          .build();
      }

      return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
        .header("Content-Disposition", "attachment; filename=" + uriEncodedFilename).build();
    } catch (NoSuchEntityException e) {
      String name = doZip(path);

      return Response.ok(tempFileService.getInputStreamFromFile(name))
        .header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
    }
  }

  @GET
  @Path("/file/{path:.*}")
  public Mica.FileDto getFile(@PathParam("path") String path, @QueryParam("key") String shareKey, @QueryParam("recursive") @DefaultValue("false") boolean recursive) {
    return doGetFile(path, shareKey, recursive);
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
