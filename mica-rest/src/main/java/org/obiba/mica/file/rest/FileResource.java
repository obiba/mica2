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
import org.obiba.mica.core.domain.PersistableWithAttachments;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

@Component
@Scope("request")
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
    if (persistable != null) {
      attachment = persistable.findAttachmentById(fileId);

      return Response.ok(gitService.readFileHead(persistable, attachment.getId()))
        .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
    }

    return Response.ok(fileStoreService.getFile(attachment.getFileReference()))
      .header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"").build();
  }
}
