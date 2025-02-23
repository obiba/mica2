/*
 * Copyright (c) 2019 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import com.google.common.base.Joiner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.obiba.mica.security.ShiroAuditorAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class AuditInterceptor implements ContainerResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);

  private static final String LOG_FORMAT = "{}";

  private static final String WS_ROOT = "/ws";


  @Inject
  private ShiroAuditorAware auditorAware;

  @Context
  private HttpServletRequest servletRequest;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    throws IOException {

    logServerError(requestContext, responseContext);
    logClientError(requestContext, responseContext);
    logInfo(requestContext, responseContext);
  }

  private String getArguments(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.clear();
    MDC.put("username", auditorAware.getCurrentAuditor().get());
    MDC.put("status", responseContext.getStatus() + "");
    MDC.put("method", requestContext.getMethod());
    MDC.put("ip", ClientIPUtils.getClientIP(requestContext, servletRequest));

    Date d = requestContext.getDate();
    if(d != null) MDC.put("time", (LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() - d.getTime()) + "");

    StringBuilder sb = new StringBuilder("/").append(requestContext.getUriInfo().getPath(true));
    MultivaluedMap<String, String> params = requestContext.getUriInfo().getQueryParameters();
    if(!params.isEmpty()) {
      sb.append(" queryParams:").append("{");
      boolean first = true;
      for(Map.Entry<String, List<String>> kv : params.entrySet()) {
        if(first) first = false;
        else sb.append(", ");
        sb.append(kv.getKey()).append(": [").append(Joiner.on(", ").join(kv.getValue())).append("]");
      }
      sb.append("}");
    }

    return sb.toString();
  }

  private void logServerError(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if(!log.isErrorEnabled()) return;
    if(responseContext.getStatus() < HttpStatus.INTERNAL_SERVER_ERROR.value()) return;

    log.error(LOG_FORMAT, getArguments(requestContext, responseContext));
  }

  private void logClientError(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if(!log.isWarnEnabled()) return;
    if(responseContext.getStatus() < HttpStatus.BAD_REQUEST.value()) return;
    if(responseContext.getStatus() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) return;

    log.warn(LOG_FORMAT, getArguments(requestContext, responseContext));
  }

  private void logInfo(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if(!log.isInfoEnabled()) return;
    if(responseContext.getStatus() >= HttpStatus.BAD_REQUEST.value()) return;

    boolean logged = false;
    if(responseContext.getStatus() == HttpStatus.CREATED.value()) {
      String resourceUri = responseContext.getHeaderString(HttpHeaders.LOCATION);
      if(resourceUri != null) {
        String args = getArguments(requestContext, responseContext);
        String path = resourceUri.substring(resourceUri.indexOf(WS_ROOT) + WS_ROOT.length());
        MDC.put("created", path);
        log.info(LOG_FORMAT, args);
        logged = true;
      }
    }

    if(!logged) {
      log.info(LOG_FORMAT, getArguments(requestContext, responseContext));
    }
  }
}
