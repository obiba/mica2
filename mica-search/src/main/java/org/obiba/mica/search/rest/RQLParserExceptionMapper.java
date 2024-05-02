/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import net.jazdw.rql.parser.RQLParserException;

@Provider
public class RQLParserExceptionMapper implements ExceptionMapper<RQLParserException> {

  @Override
  public Response toResponse(RQLParserException exception) {
    return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
  }

}
