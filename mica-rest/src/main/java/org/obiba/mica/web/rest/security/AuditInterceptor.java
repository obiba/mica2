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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.obiba.mica.security.ShiroAuditorAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Joiner;

public class AuditInterceptor implements ContainerResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);

  private static final String LOG_FORMAT = "{}";

  private static final String WS_ROOT = "/ws";

  @Inject
  private ShiroAuditorAware auditorAware;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    throws IOException {

    logServerError(requestContext, responseContext);
    logClientError(requestContext, responseContext);
    logInfo(requestContext, responseContext);
  }

  private String getArguments(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.clear();
    MDC.put("username", auditorAware.getCurrentAuditor());
    MDC.put("status", responseContext.getStatus() + "");
    MDC.put("method", requestContext.getMethod());

    Date d = requestContext.getDate();
    if(d != null) MDC.put("time", (DateTime.now().getMillis() - d.getTime()) + "");

    StringBuilder sb = new StringBuilder("/").append(requestContext.getUriInfo().getPath(true));
    MultivaluedMap<String, String> params = requestContext.getUriInfo().getQueryParameters();
    if(params.size() > 0) {
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
    if(responseContext.getStatus() < HttpStatus.SC_INTERNAL_SERVER_ERROR) return;

    log.error(LOG_FORMAT, getArguments(requestContext, responseContext));
  }

  private void logClientError(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if(!log.isWarnEnabled()) return;
    if(responseContext.getStatus() < HttpStatus.SC_BAD_REQUEST) return;
    if(responseContext.getStatus() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) return;

    log.warn(LOG_FORMAT, getArguments(requestContext, responseContext));
  }

  private void logInfo(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if(!log.isInfoEnabled()) return;
    if(responseContext.getStatus() >= HttpStatus.SC_BAD_REQUEST) return;

    boolean logged = false;
    if(responseContext.getStatus() == HttpStatus.SC_CREATED) {
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
