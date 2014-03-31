package org.obiba.mica.web.filter.gzip;

import javax.servlet.ServletException;

public class GzipResponseHeadersNotModifiableException extends ServletException {

  private static final long serialVersionUID = 6990846281890168790L;

  public GzipResponseHeadersNotModifiableException(String message) {
    super(message);
  }
}
