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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

public class CSRFInterceptor implements ContainerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(CSRFInterceptor.class);

  private static final String HOST_HEADER = "Host";

  private static final String REFERER_HEADER = "Referer";

  private static final String USER_AGENT_HEADER = "User-Agent";

  private static final Pattern localhostPattern = Pattern.compile("^http[s]?://localhost:");

  private static final Pattern loopbackhostPattern = Pattern.compile("^http[s]?://127\\.0\\.0\\.1:");

  private final boolean productionMode;

  private final List<String> csrfAllowedHosts;

  private final List<String> csrfAllowedAgents;

  public CSRFInterceptor(boolean productionMode, String csrfAllowedHosts, String csrfAllowedAgents) {
    this.productionMode = productionMode;
    this.csrfAllowedHosts = Strings.isNullOrEmpty(csrfAllowedHosts) ? Lists.newArrayList() : Splitter.on(",").splitToList(csrfAllowedHosts.trim());
    this.csrfAllowedAgents = Strings.isNullOrEmpty(csrfAllowedAgents) ? Lists.newArrayList() : Splitter.on(",").splitToList(csrfAllowedAgents.trim());
  }

  @Override
  public void filter(ContainerRequestContext requestContext)
    throws IOException {
    if (!productionMode || csrfAllowedHosts.contains("*")) return;

    String host = requestContext.getHeaderString(HOST_HEADER);
    if (matchesLocalhost(host)) return;

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
      if (csrfAllowedHosts.contains(refererHostPort)) return;

      boolean forbidden = !referer.startsWith(String.format("https://%s/", host));
      if (forbidden) {
        log.warn("CSRF detection: Host={}, Referer={}", host, referer);
        log.info(">> You can add {} to csrf.allowed setting", refererHostPort);
        throw new ForbiddenException();
      }
    } else {
      String userAgent = requestContext.getHeaderString(USER_AGENT_HEADER);
      if (Strings.isNullOrEmpty(userAgent) || !matchesUserAgent(userAgent)) {
        log.warn("CSRF detection: Host={}, User-Agent={}", host, userAgent);
        log.info(">> Ensure 'Referer' HTTP header is set or allow this 'User-Agent' with 'csrf.allowed-agents' setting");
        throw new ForbiddenException("CSRF error");
      }
    }
  }

  private boolean matchesLocalhost(String host) {
    return localhostPattern.matcher(host).matches()
      || loopbackhostPattern.matcher(host).matches()
      || host.startsWith("localhost:")
      || host.startsWith("127.0.0.1:");
  }

  private boolean matchesUserAgent(String userAgent) {
    return csrfAllowedAgents.stream().anyMatch(ua -> userAgent.toLowerCase().contains(ua.toLowerCase()));
  }

}
