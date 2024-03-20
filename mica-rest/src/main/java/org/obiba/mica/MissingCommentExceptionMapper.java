package org.obiba.mica;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.core.service.MissingCommentException;
import org.obiba.web.model.ErrorDtos;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class MissingCommentExceptionMapper extends AbstractErrorDtoExceptionMapper<MissingCommentException> {
  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(MissingCommentException e) {
    return ErrorDtos.ClientErrorDto.newBuilder()
      .setCode(getStatus().getStatusCode())
      .setMessageTemplate("server.error.document-comment")
      .setMessage(e.getMessage())
      .build();
  }
}
