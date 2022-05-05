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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class CSRFInterceptor implements ContainerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(CSRFInterceptor.class);

  private static final String HOST_HEADER = "Host";

  private static final String REFERER_HEADER = "Referer";

  private final String serverPort;

  private final boolean productionMode;

  private final List<String> csrfAllowed;

  public CSRFInterceptor(String port, boolean productionMode, String csrfAllowed) {
    serverPort = port;
    this.productionMode = productionMode;
    this.csrfAllowed = Strings.isNullOrEmpty(csrfAllowed) ? Lists.newArrayList() : Splitter.on(",").splitToList(csrfAllowed.trim());
  }

  @Override
  public void filter(ContainerRequestContext requestContext)
    throws IOException {
    if (!productionMode || csrfAllowed.contains("*")) return;

    String host = requestContext.getHeaderString(HOST_HEADER);
    String referer = requestContext.getHeaderString(REFERER_HEADER);
    if (referer != null) {
      String refererHostPort = "";
      try {
        URI refererURI = URI.create(referer);
        refererHostPort = refererURI.getHost() + (refererURI.getPort() > 0 ? ":" + refererURI.getPort() : "");
      } catch (Exception e) {
        // malformed url
      }
      // explicitly ok
      if (csrfAllowed.contains(refererHostPort)) return;

      String localhost = String.format("localhost:%s", serverPort);
      String loopbackhost = String.format("127.0.0.1:%s", serverPort);
      boolean forbidden = false;
      if (localhost.equals(host) || loopbackhost.equals(host)) {
        if (!referer.startsWith(String.format("http://%s/", host)))
          forbidden = true;
      } else if (!referer.startsWith(String.format("https://%s/", host))) {
        forbidden = true;
      }

      if (forbidden) {
        log.warn("CSRF detection: Host={}, Referer={}", host, referer);
        log.info(">> You can add {} to csrf.allowed setting", refererHostPort);
        throw new ForbiddenException();
      }
    }
  }

}
