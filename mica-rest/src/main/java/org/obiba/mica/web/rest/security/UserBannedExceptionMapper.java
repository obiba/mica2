/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.shiro.web.filter.UserBannedException;
import org.obiba.web.model.ErrorDtos;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UserBannedExceptionMapper extends AbstractErrorDtoExceptionMapper<UserBannedException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.FORBIDDEN;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(UserBannedException e) {
    return ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("error.userBanned") //
      .setMessage("User is banned") //
      .build();
  }

}
