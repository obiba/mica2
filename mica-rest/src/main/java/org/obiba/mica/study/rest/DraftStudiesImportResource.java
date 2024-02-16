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

import java.io.IOException;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.study.service.StudyPackageImportService;

@Path("/draft/studies/_import")
public class DraftStudiesImportResource {

  @Inject
  private StudyPackageImportService studyPackageImportService;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @RequiresPermissions("/draft/individual-study:ADD")
  public Response importZip(@Context HttpServletRequest request,
    @QueryParam("publish") @DefaultValue("false") boolean publish) throws FileUploadException, IOException {
    FileItem uploadedFile = getUploadedFile(request);
    studyPackageImportService.importZip(uploadedFile.getInputStream(), publish);

    return Response.ok().build();
  }

  /**
   * Returns the first {@code FileItem} that is represents a file upload field. If no such field exists, this method
   * returns null
   *
   * @param request
   * @return
   * @throws FileUploadException
   */
  FileItem getUploadedFile(HttpServletRequest request) throws FileUploadException {
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    for(FileItem fileItem : upload.parseRequest(request)) {
      if(!fileItem.isFormField()) {
        return fileItem;
      }
    }

    return null;
  }

}
