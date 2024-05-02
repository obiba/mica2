package org.obiba.mica.web.rest.security;

import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;

public class ClientIPUtils {

  public static final String[] VALID_IP_HEADER_CANDIDATES = {
    "X-Forwarded-For",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_X_FORWARDED_FOR",
    "HTTP_X_FORWARDED",
    "HTTP_X_CLUSTER_CLIENT_IP",
    "HTTP_CLIENT_IP",
    "HTTP_FORWARDED_FOR",
    "HTTP_FORWARDED",
    "HTTP_VIA",
    "REMOTE_ADDR"};

  public static String getClientIP(ContainerRequestContext requestContext, HttpServletRequest servletRequest) {
    String ip = "";

    for (String ipHeader : VALID_IP_HEADER_CANDIDATES) {
      ip = requestContext.getHeaders().keySet().stream()
        .filter(ipHeader::equalsIgnoreCase)
        .map(requestContext::getHeaderString)
        .findFirst().orElse("");
      if (!Strings.isNullOrEmpty(ip)) break;
    }

    if (Strings.isNullOrEmpty(ip))
      ip = servletRequest.getRemoteAddr();

    return ip;
  }

  public static String getClientIP(HttpServletRequest servletRequest) {
    String ip = "";

    for (String ipHeader : VALID_IP_HEADER_CANDIDATES) {
      ip = servletRequest.getHeaders(ipHeader).nextElement();
      if (!Strings.isNullOrEmpty(ip)) break;
    }

    if (Strings.isNullOrEmpty(ip))
      ip = servletRequest.getRemoteAddr();

    return ip;
  }
}
