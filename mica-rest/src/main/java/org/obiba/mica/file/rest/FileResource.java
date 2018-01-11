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

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.PersistableWithAttachments;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@RequiresAuthentication
public class FileResource {

  @Inject
  private GitService gitService;

  @Inject
  private FileStoreService fileStoreService;

  private PersistableWithAttachments persistable;

  private String fileId;

  private Attachment attachment;

  public void setPersistable(PersistableWithAttachments persistable) {
    this.persistable = persistable;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public void setAttachment(Attachment attachment) {
    this.attachment = attachment;
  }

  @GET
  @Path("/_download")
  @Timed
  public Response download() throws IOException {
    if(persistable != null) {
      attachment = persistable.findAttachmentById(fileId);

      return Response.ok(gitService.readFileHead(persistable, attachment.getId()))
        .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
    }

    return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
      .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }
}
