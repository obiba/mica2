package org.obiba.mica.file.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.PersistableWithAttachments;
import org.obiba.mica.core.service.GitService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@RequiresAuthentication
public class FileResource {

  @Inject
  private GitService gitService;

  private PersistableWithAttachments persistable;

  private String fileId;

  public void setPersistable(PersistableWithAttachments persistable) {
    this.persistable = persistable;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  @GET
  @Path("/_download")
  @Timed
  public Response download() {
    Attachment attachment = persistable.findAttachmentById(fileId);
    return Response.ok(gitService.readFileHead(persistable.getId(), fileId))
        .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }

}
