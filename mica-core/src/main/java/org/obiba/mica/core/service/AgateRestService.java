/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public abstract class AgateRestService {

  private static final Logger log = LoggerFactory.getLogger(AgateRestService.class);

  protected static final int DEFAULT_HTTPS_PORT = 443;

  private static final long MAX_IDLE_TIME = 30000L;

  protected static final String APPLICATION_AUTH_HEADER = "X-App-Auth";

  protected static final String APPLICATION_AUTH_SCHEMA = "Basic";

  protected static final String DEFAULT_REST_PREFIX = "/ws";

  @Inject
  protected Environment env;

  private HttpComponentsClientHttpRequestFactory httpRequestFactory;

  private String agateUrl;

  private String serviceName;

  private String serviceKey;

  @PostConstruct
  public abstract void init();

  protected void initInternal() {
    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "agate.");
    agateUrl = propertyResolver.getProperty("url");
    serviceName = propertyResolver.getProperty("application.name");
    serviceKey = propertyResolver.getProperty("application.key");
  }

  protected String getApplicationAuth() {
    String token = serviceName + ":" + serviceKey;
    return APPLICATION_AUTH_SCHEMA + " " + Base64.encodeToString(token.getBytes());
  }

  protected RestTemplate newRestTemplate() {
    log.info("Connecting to Agate: {}", agateUrl);
    if(agateUrl.toLowerCase().startsWith("https://")) {
      if(httpRequestFactory == null) {
        httpRequestFactory = new HttpComponentsClientHttpRequestFactory(createHttpClient());
      }
      return new RestTemplate(httpRequestFactory);
    } else {
      return new RestTemplate();
    }
  }

  protected String getAgateUrl() {
    return agateUrl;
  }

  protected String getServiceName() {
    return serviceName;
  }

  protected String getServiceKey() {
    return serviceKey;
  }

  protected HttpComponentsClientHttpRequestFactory getHttpRequestFactory() {
    return httpRequestFactory;
  }

  protected HttpClient createHttpClient() {
    HttpClientBuilder builder = HttpClientBuilder.create();
    try {
      builder.setSSLSocketFactory(getSocketFactory());
      // if component not specified, will use the default

    } catch(NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }

    return builder.build();
  }

  /**
   * Do not check anything from the remote host (Agate server is trusted).
   *
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   */
  private SSLConnectionSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
    // Accepts any SSL certificate
    TrustManager tm = new X509TrustManager() {

      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] { tm }, null);

    return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
  }
}
