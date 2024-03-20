/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.micaConfig.service.TaxonomyNotFoundException;
import org.obiba.web.model.ErrorDtos;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class TaxonomyNotFoundExceptionMapper extends AbstractErrorDtoExceptionMapper<TaxonomyNotFoundException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.NOT_FOUND;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(TaxonomyNotFoundException e) {
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
