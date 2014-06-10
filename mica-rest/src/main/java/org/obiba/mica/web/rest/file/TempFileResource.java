package org.obiba.mica.web.rest.file;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.TempFileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
public class TempFileResource {

  @Inject
  private TempFileService tempFileService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.TempFileDto getMetadata() throws IOException {
    return dtos.asDto(tempFileService.getMetadata(id));
  }

  @GET
  @Path("/_download")
  @Timed
  public Response download() throws IOException {
    TempFile tempFile = tempFileService.getMetadata(id);
    return Response.ok(tempFileService.getContent(id))
        .header("Content-Disposition", "attachment; filename=\"" + tempFile.getName() + "\"").build();
  }

  @DELETE
  @Timed
  public Response delete() {
    tempFileService.delete(id);
    return Response.noContent().build();
  }
}
