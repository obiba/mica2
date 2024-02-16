/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.web.model.ErrorDtos;

@Provider
public class NoSuchDataAccessRequestExceptionMapper
  extends AbstractErrorDtoExceptionMapper<NoSuchDataAccessRequestException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.NOT_FOUND;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(NoSuchDataAccessRequestException e) {
    ErrorDtos.ClientErrorDto.Builder builder = ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("server.error.data-access-request.not-found") //
      .setMessage(e.getMessage());

    if (e.hasRequestId()) {
      builder.addArguments(e.getRequestId());
    }

    return builder.build();
  }
}
