/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.obiba.magma.NoSuchValueTableException;

@Provider
public class NoSuchValueTableExceptionMapper implements ExceptionMapper<NoSuchValueTableException> {

  @Override
  public Response toResponse(NoSuchValueTableException exception) {
    return Response.status(Status.NOT_FOUND).entity(exception.getMessage()).build();
  }

}
