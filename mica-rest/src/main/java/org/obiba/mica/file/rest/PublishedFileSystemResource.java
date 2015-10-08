package org.obiba.mica.file.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javafx.util.Pair;

@Component
@Path("/")
public class PublishedFileSystemResource {

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/fs/{path:.*}")
  public Mica.AttachmentDto getAttachment(@PathParam("path") String pathWithName) {
    Pair<String, String> pathName = FileSystemService.extractPathName(pathWithName);
    return dtos
      .asDto(fileSystemService.getPublishedAttachment(String.format("/%s", pathName.getKey()), pathName.getValue()));
  }
}
