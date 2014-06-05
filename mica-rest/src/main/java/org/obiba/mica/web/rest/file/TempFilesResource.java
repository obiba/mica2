package org.obiba.mica.web.rest.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

@Path("/files/temp")
public class TempFilesResource {

  private static final Logger log = LoggerFactory.getLogger(TempFilesResource.class);

  @Inject
  private ApplicationContext applicationContext;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Timed
  public Response upload(@FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @Context UriInfo uriInfo) {

    log.debug("name: {}", fileDetail.getName());
    log.debug("fileName: {}", fileDetail.getFileName());
    log.debug("param: {}", fileDetail.getParameters());
    log.debug("size: {}", fileDetail.getSize());
    log.debug("type: {}", fileDetail.getType());
    try {
      log.debug("file content: {}", CharStreams.toString(new InputStreamReader(uploadedInputStream, Charsets.UTF_8)));
    } catch(IOException e) {
      e.printStackTrace();
    }

    String fileId = "1";
    return Response.created(uriInfo.getBaseUriBuilder().path(TempFilesResource.class, "file").build(fileId)).build();
  }

  @Path("/{id}")
  public TempFileResource file(@PathParam("id") String id) {
    TempFileResource tempFileResource = applicationContext.getBean(TempFileResource.class);
    tempFileResource.setId(id);
    return tempFileResource;
  }

}
