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

import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.web.model.ErrorDtos;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class UncheckedTimeoutExceptionMapper extends AbstractErrorDtoExceptionMapper<UncheckedTimeoutException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.GATEWAY_TIMEOUT;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(UncheckedTimeoutException e) {
    return ErrorDtos.ClientErrorDto.newBuilder()
      .setCode(getStatus().getStatusCode())
      .setMessageTemplate("server.error.search.timeout")
      .setMessage(e.getMessage())
      .build();
  }
}
