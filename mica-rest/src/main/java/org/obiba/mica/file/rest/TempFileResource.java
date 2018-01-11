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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Scope("request")
@RequiresPermissions({"/files:UPLOAD"})
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
