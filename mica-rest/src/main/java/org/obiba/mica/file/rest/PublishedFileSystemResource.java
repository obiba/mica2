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

import com.codahale.metrics.annotation.Timed;
import com.google.common.io.Files;
import java.io.UnsupportedEncodingException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.file.support.FileMediaType;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.springframework.web.util.UriUtils;

@Component
@Path("/")
@RequiresAuthentication
public class PublishedFileSystemResource extends AbstractFileSystemResource {

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private TempFileService tempFileService;

  @Override
  protected boolean isPublishedFileSystem() {
    return true;
  }

  @GET
  @Path("/file-dl/{path:.*}")
  @Timed
  public Response downloadFile(@PathParam("path") String path,
    @QueryParam("inline") @DefaultValue("false") boolean inline) {

    try {
      Attachment attachment = doGetAttachment(path);
      String filename = attachment.getName();
      String uriEncodedFilename = UriUtils.encode(filename, "UTF-8");

      if (inline) {
        return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
          .header("Content-Disposition", "inline; filename=\"" + uriEncodedFilename + "\"")
          .type(FileMediaType.type(Files.getFileExtension(filename)))
          .build();
      }

      return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
        .header("Content-Disposition", "attachment; filename*=" + uriEncodedFilename).build();
    } catch (NoSuchEntityException | UnsupportedEncodingException e) {
      String name = doZip(path);

      return Response.ok(tempFileService.getInputStreamFromFile(name))
        .header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
    }
  }

  @GET
  @Path("/file/{path:.*}")
  @Timed
  public Mica.FileDto getFile(@PathParam("path") String path) {
    return doGetFile(path);
  }
}
