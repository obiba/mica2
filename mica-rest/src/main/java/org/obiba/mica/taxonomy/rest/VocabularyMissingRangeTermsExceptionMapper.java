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
import org.obiba.mica.micaConfig.service.VocabularyMissingRangeTermsException;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.web.model.ErrorDtos;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class VocabularyMissingRangeTermsExceptionMapper
  extends AbstractErrorDtoExceptionMapper<VocabularyMissingRangeTermsException> {

  @Override
  protected Response.Status getStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  protected ErrorDtos.ClientErrorDto getErrorDto(VocabularyMissingRangeTermsException e) {
    Vocabulary vocabulary = e.getVocabulary();
    ErrorDtos.ClientErrorDto.Builder builder =
      ErrorDtos.ClientErrorDto.newBuilder().setCode(getStatus().getStatusCode());

    if (vocabulary != null) {
      builder.setMessageTemplate("server.error.taxonomy.range-criterion-missing-terms");
      builder.addArguments(vocabulary.getName());
    }

    return builder.build();
  }
}
