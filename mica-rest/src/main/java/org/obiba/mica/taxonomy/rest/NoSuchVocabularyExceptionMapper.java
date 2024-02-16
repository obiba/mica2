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
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.obiba.opal.core.cfg.NoSuchVocabularyException;

@Provider
public class NoSuchVocabularyExceptionMapper implements ExceptionMapper<NoSuchVocabularyException> {

  @Override
  public Response toResponse(NoSuchVocabularyException exception) {
    return Response.status(Status.NOT_FOUND).build();
  }

}
