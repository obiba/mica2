/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.obiba.mica.study.service.StudyPackageImportService;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@Path("/draft/studies/_import")
public class DraftStudiesImportResource {

  @Inject
  private StudyPackageImportService studyPackageImportService;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RequiresPermissions("/draft/individual-study:ADD")
  public Response importZip(@FormDataParam("file") InputStream file,
    @QueryParam("publish") @DefaultValue("false") boolean publish) throws IOException {
    studyPackageImportService.importZip(file, publish);
    return Response.ok().build();
  }

}
