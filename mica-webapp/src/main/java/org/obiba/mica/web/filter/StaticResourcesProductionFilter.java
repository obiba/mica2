/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

/**
 * This filter is used in production, to serve static resources generated by "grunt build".
 * It is configured to serve resources from the "dist" directory, which is the Grunt
 * destination directory.
 */
public class StaticResourcesProductionFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to initialize
  }

  @Override
  public void destroy() {
    // Nothing to destroy
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String contextPath = httpRequest.getContextPath();
    String requestURI = httpRequest.getRequestURI();
    requestURI = StringUtils.substringAfter(requestURI, contextPath);
    if(StringUtils.equals("/", requestURI)) {
      requestURI = "/index.html";
    }
    String newURI = "/dist" + requestURI;
    request.getRequestDispatcher(newURI).forward(request, response);
  }
}
