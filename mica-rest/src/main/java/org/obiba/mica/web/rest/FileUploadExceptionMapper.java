/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.fileupload.FileUploadException;

@Provider
public class FileUploadExceptionMapper implements ExceptionMapper<FileUploadException> {

  @Override
  public Response toResponse(FileUploadException exception) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
  }
}
