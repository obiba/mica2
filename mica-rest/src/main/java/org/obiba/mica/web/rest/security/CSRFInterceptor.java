/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

public class CSRFInterceptor implements ContainerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(CSRFInterceptor.class);

  private static final String HOST_HEADER = "Host";

  private static final String REFERER_HEADER = "Referer";

  @Override
  public void filter(ContainerRequestContext requestContext)
    throws IOException {
    String host = requestContext.getHeaderString(HOST_HEADER);
    String referer = requestContext.getHeaderString(REFERER_HEADER);
    if (referer != null) {
      boolean forbidden = !referer.startsWith(String.format("http://%s/", host)) && !referer.startsWith(String.format("https://%s/", host));
      if (forbidden) {
        log.warn("CSRF detection: Host={}, Referer={}", host, referer);
        throw new ForbiddenException();
      }
    }
  }

}
