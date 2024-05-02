/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.micaConfig.service.InvalidFormSchemaException;
import org.obiba.web.model.ErrorDtos;

@Provider
public class InvalidFormSchemaExceptionMapper extends AbstractErrorDtoExceptionMapper<InvalidFormSchemaException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(InvalidFormSchemaException e){
    return ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("server.error.data-access-form.invalid-schema") //
      .setMessage(e.getMessage()) //
      .build();
  }
}
