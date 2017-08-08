package org.obiba.mica.taxonomy.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.micaConfig.service.TaxonomyNotFoundException;
import org.obiba.web.model.ErrorDtos;

import com.google.protobuf.GeneratedMessage;

@Provider
public class TaxonomyNotFoundExceptionMapper extends AbstractErrorDtoExceptionMapper<TaxonomyNotFoundException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.NOT_FOUND;
  }

  @Override
  protected GeneratedMessage.ExtendableMessage<?> getErrorDto(TaxonomyNotFoundException e) {
    ErrorDtos.ClientErrorDto.Builder builder =
      ErrorDtos.ClientErrorDto.newBuilder().setCode(getStatus().getStatusCode());

    String taxonomyName = e.getTaxonomyName();

    if (taxonomyName != null) {
      builder.setMessageTemplate("server.error.taxonomy.name-not-found");
      builder.addArguments(taxonomyName);
    }

    return builder.build();
  }
}
