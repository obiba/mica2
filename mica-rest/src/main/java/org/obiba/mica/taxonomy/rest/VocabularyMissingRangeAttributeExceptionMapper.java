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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.obiba.jersey.exceptionmapper.AbstractErrorDtoExceptionMapper;
import org.obiba.mica.micaConfig.service.VocabularyMissingRangeAttributeException;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.web.model.ErrorDtos;

@Provider
public class VocabularyMissingRangeAttributeExceptionMapper
  extends AbstractErrorDtoExceptionMapper<VocabularyMissingRangeAttributeException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(VocabularyMissingRangeAttributeException e) {
    Vocabulary vocabulary = e.getVocabulary();
    ErrorDtos.ClientErrorDto.Builder builder =
      ErrorDtos.ClientErrorDto.newBuilder().setCode(getStatus().getStatusCode());

    if (vocabulary != null) {
      builder.setMessageTemplate("server.error.taxonomy.range-criterion-missing-range-attribute");
      builder.addArguments(vocabulary.getName());
    }

    return builder.build();
  }
}
