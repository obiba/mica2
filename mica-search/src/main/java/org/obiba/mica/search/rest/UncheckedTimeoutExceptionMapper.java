/*
 *
 *  * Copyright (c) 2017 OBiBa. All rights reserved.
 *  *
 *  * This program and the accompanying materials
 *  * are made available under the terms of the GNU Public License v3.0.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.obiba.mica.search.rest;

import com.google.common.util.concurrent.UncheckedTimeoutException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UncheckedTimeoutExceptionMapper implements ExceptionMapper<UncheckedTimeoutException> {

  @Override
  public Response toResponse(UncheckedTimeoutException exception) {
    return Response.status(Response.Status.GATEWAY_TIMEOUT).entity(exception.getMessage()).build();
  }
}
