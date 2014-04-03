/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.mica.web.rest.provider.exceptionmapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    //TODO use ErrorDto
    log.error("UnhandledException", exception);
    return Response.status(INTERNAL_SERVER_ERROR).type(TEXT_PLAIN).entity(exception.getMessage()).build();
  }

}
