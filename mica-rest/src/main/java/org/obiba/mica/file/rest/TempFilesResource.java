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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.service.TempFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Path("/files/temp")
@RequiresPermissions({"/files:UPLOAD"})
public class TempFilesResource {

  private static final Logger log = LoggerFactory.getLogger(TempFilesResource.class);

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private TempFileService tempFileService;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Timed
  public Response upload(@FormDataParam("file") InputStream file, @FormDataParam("file") FormDataContentDisposition fileDetails, @Context UriInfo uriInfo)
    throws IOException, FileUploadException {

    if (file == null) throw new FileUploadException("Failed to extract file item from request");
    TempFile tempFile = tempFileService.addTempFile(fileDetails.getFileName(), file);
    URI location = uriInfo.getBaseUriBuilder().path(TempFilesResource.class).path(TempFilesResource.class, "file")
      .build(tempFile.getId());

    return Response.created(location).build();
  }

  @Path("/{id}")
  public TempFileResource file(@PathParam("id") String id) {
    TempFileResource tempFileResource = applicationContext.getBean(TempFileResource.class);
    tempFileResource.setId(id);
    return tempFileResource;
  }
}
