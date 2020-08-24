/*
 * Copyright (c) 2019 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter is used to mitigate the "Web Application Potentially Vulnerable to Clickjacking". See https://www.tenable.com/plugins/nessus/85582
 * </p>
 */
public class ClickjackingHttpHeadersFilter implements Filter {

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
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    httpResponse.setHeader("X-Frame-Options", "DENY");
    httpResponse.setHeader("Content-Security-Policy", "frame-ancestors 'none'");

    chain.doFilter(request, response);
  }
}
