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

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public abstract class AgateRestService implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(AgateRestService.class);

  protected static final int DEFAULT_HTTPS_PORT = 443;

  private static final long MAX_IDLE_TIME = 30000L;

  protected static final String APPLICATION_AUTH_HEADER = "X-App-Auth";

  protected static final String APPLICATION_AUTH_SCHEMA = "Basic";

  protected static final String DEFAULT_REST_PREFIX = "/ws";

  @Inject
  protected Environment env;

  @Inject
  protected AgateServerConfigService agateServerConfigService;

  private HttpComponentsClientHttpRequestFactory httpRequestFactory;


  @Override
  public void afterPropertiesSet() throws Exception {
  }

  protected String getApplicationName() {
    return agateServerConfigService.getServiceName();
  }

  protected String getApplicationAuth() {
    String token = agateServerConfigService.buildToken();
    return APPLICATION_AUTH_SCHEMA + " " + Base64.encodeToString(token.getBytes());
  }

  protected RestTemplate newRestTemplate() {

    TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

    PoolingHttpClientConnectionManager connectionManager = null;
    try {
      connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
          .setSslContext(SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build())
          .setTlsVersions(TLS.V_1_3, TLS.V_1_2)
          .build())
        .setDefaultSocketConfig(SocketConfig.custom()
          .setSoTimeout(Timeout.ofSeconds(5))
          .build())
        .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
        .setConnPoolPolicy(PoolReusePolicy.LIFO)
        .setDefaultConnectionConfig(ConnectionConfig.custom()
          .setTimeToLive(TimeValue.ofMinutes(1L))
          .setConnectTimeout(Timeout.ofSeconds(5))
          .build())
        .build();
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
      throw new RuntimeException(e);
    }

    CloseableHttpClient client = HttpClients.custom()
      .setConnectionManager(connectionManager)
      .setDefaultRequestConfig(RequestConfig.custom()
        .setResponseTimeout(Timeout.ofSeconds(5))
        .setCookieSpec(StandardCookieSpec.STRICT)
        .build())
      .build();

    HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);

    return new RestTemplate(httpComponentsClientHttpRequestFactory);
  }

  protected HttpComponentsClientHttpRequestFactory getHttpRequestFactory() {
    return httpRequestFactory;
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
    sslContext.init(null, new TrustManager[]{tm}, null);

    return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
  }
}
