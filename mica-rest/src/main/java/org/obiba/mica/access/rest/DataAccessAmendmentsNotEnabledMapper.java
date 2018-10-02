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

import com.google.protobuf.GeneratedMessage;
import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.micaConfig.DataAccessAmendmentsNotEnabled;
import org.obiba.web.model.ErrorDtos;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class DataAccessAmendmentsNotEnabledMapper extends AbstractErrorDtoExceptionMapper<DataAccessAmendmentsNotEnabled> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected GeneratedMessage.ExtendableMessage<?> getErrorDto(DataAccessAmendmentsNotEnabled e) {
    return ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("server.error.data-access-amendment.not-enabled") //
      .setMessage(e.getMessage()) //
      .build();
  }
}

