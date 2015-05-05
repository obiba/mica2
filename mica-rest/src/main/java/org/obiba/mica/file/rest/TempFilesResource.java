package org.obiba.mica.file.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.obiba.mica.core.security.Roles;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.TempFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/files/temp")
@RequiresRoles(Roles.MICA_ADMIN)
public class TempFilesResource {

  private static final Logger log = LoggerFactory.getLogger(TempFilesResource.class);

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private TempFileService tempFileService;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Timed
  public Response upload(@FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @Context UriInfo uriInfo) throws IOException {

    TempFile tempFile = tempFileService.addTempFile(fileDetail.getFileName(), uploadedInputStream);
    //TODO make this work
    // URI location = uriInfo.getBaseUriBuilder().path(TempFilesResource.class, "file").build(tempFile.getId());
    URI location = uriInfo.getBaseUriBuilder().path("/files/temp/" + tempFile.getId()).build();
    return Response.created(location).build();
  }

  @Path("/{id}")
  public TempFileResource file(@PathParam("id") String id) {
    TempFileResource tempFileResource = applicationContext.getBean(TempFileResource.class);
    tempFileResource.setId(id);
    return tempFileResource;
  }

}
