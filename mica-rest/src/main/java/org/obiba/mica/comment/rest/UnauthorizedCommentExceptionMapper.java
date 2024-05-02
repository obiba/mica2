package org.obiba.mica.comment.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.core.domain.UnauthorizedCommentException;
import org.obiba.web.model.ErrorDtos;

@Provider
public class UnauthorizedCommentExceptionMapper extends AbstractErrorDtoExceptionMapper<UnauthorizedCommentException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(UnauthorizedCommentException e) {
    return ErrorDtos.ClientErrorDto.newBuilder() //
      .setCode(getStatus().getStatusCode()) //
      .setMessageTemplate("server.error.403") //
      .setMessage(e.getMessage()) //
      .build();
  }
}
