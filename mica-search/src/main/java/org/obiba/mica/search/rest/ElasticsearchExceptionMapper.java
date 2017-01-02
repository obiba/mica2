/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.elasticsearch.ElasticsearchException;
import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.web.model.ErrorDtos;

import com.google.protobuf.GeneratedMessage;

@Provider
public class ElasticsearchExceptionMapper extends AbstractErrorDtoExceptionMapper<ElasticsearchException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected GeneratedMessage.ExtendableMessage<?> getErrorDto(ElasticsearchException e) {
    return ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("server.error.search.failed") //
      .setMessage(e.getMessage()) //
      .addArguments(e.getRootCause().getMessage()) //
      .build(); //
  }
}
