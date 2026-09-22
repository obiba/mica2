/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.web.model.ErrorDtos;

@Provider
public class RateLimitExceededExceptionMapper extends AbstractErrorDtoExceptionMapper<RateLimitExceededException> {

  @Override
  public Response toResponse(RateLimitExceededException e) {
    return Response.fromResponse(super.toResponse(e))
      .header(HttpHeaders.RETRY_AFTER, e.getRetryAfterSeconds())
      .build();
  }

  @Override
  protected Response.Status getStatus() {
    return Response.Status.TOO_MANY_REQUESTS;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(RateLimitExceededException e) {
    return ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("server.error.too-many-requests") //
      .setMessage(e.getMessage()) //
      .build();
  }

}
