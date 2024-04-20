/*
 * Copyright (c) 2023 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest;

import org.springframework.web.client.HttpClientErrorException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class HttpClientErrorExceptionMapper implements ExceptionMapper<HttpClientErrorException> {
  @Override
  public Response toResponse(HttpClientErrorException e) {
    return Response.status(e.getRawStatusCode()).entity(e.getResponseBodyAsString()).build();
  }
}
