/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest;

import org.obiba.mica.micaConfig.MissingConfigurationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MissingConfigurationExceptionMapper implements ExceptionMapper<MissingConfigurationException> {

  @Override
  public Response toResponse(MissingConfigurationException exception) {
    return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
  }
}
