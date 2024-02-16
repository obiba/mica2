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
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.obiba.mica.core.domain.InvalidDocumentSetTypeException;

@Provider
public class InvalidDocumentSetTypeExceptionMapper implements ExceptionMapper<InvalidDocumentSetTypeException> {
  @Override
  public Response toResponse(InvalidDocumentSetTypeException e) {
    return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
  }
}
