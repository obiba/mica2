package org.obiba.mica.core.service;

import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

import static java.net.URLEncoder.encode;

/**
 * Service for sending e-mails.
 * <p/>
 * <p>
 * We use the @Async annotation to send e-mails asynchronously.
 * </p>
 */
@Service
public class MailService {

  private static final Logger log = LoggerFactory.getLogger(MailService.class);

  private static final int DEFAULT_HTTPS_PORT = 443;

  public static final String APPLICATION_AUTH_HEADER = "X-App-Auth";

  public static final String APPLICATION_AUTH_SCHEMA = "Basic";

  public static final String DEFAULT_REST_PREFIX = "/ws";

  public static final String DEFAULT_NOTIFICATIONS_PATH = "/notifications";

  @Inject
  private Environment env;

  private HttpComponentsClientHttpRequestFactory httpRequestFactory;

  /**
   * System default email address that sends the e-mails.
   */
  private String from;

  private String agateUrl;

  private String serviceName;

  private String serviceKey;

  @PostConstruct
  public void init() {
    from = env.getProperty("spring.mail.from");
    if(Strings.isNullOrEmpty(from)) {
      from = "mica@example.org";
    }
    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "agate.");
    agateUrl = propertyResolver.getProperty("url");
    serviceName = propertyResolver.getProperty("application.name");
    serviceKey = propertyResolver.getProperty("application.key");
  }

  @Async
  public void sendEmailToUsers(String subject, String template, Map<String, String> context, String... usernames) {
    sendEmail(subject, template, context, toRecipientFormParam("username", usernames));
  }

  @Async
  public void sendEmailToGroups(String subject, String template, Map<String, String> context, String... groups) {
    sendEmail(subject, template, context, toRecipientFormParam("group", groups));
  }

  @Async
  public void sendEmailToUsers(String subject, String text, String... usernames) {
    sendEmail(subject, text, toRecipientFormParam("username", usernames));
  }

  @Async
  public void sendEmailToGroups(String subject, String text, String... groups) {
    sendEmail(subject, text, toRecipientFormParam("group", groups));
  }

  //
  // Private methods
  //

  private String toRecipientFormParam(String type, String... recipients) {
    StringBuilder recipient = new StringBuilder();
    if(recipients != null) {
      Stream.of(recipients).forEach(rec -> {
        try {
          if(recipient.length() > 0) recipient.append("&");
          recipient.append(type).append("=").append(encode(rec, "UTF-8"));
        } catch(UnsupportedEncodingException ignored) {
        }
      });
    }
    return recipient.toString();
  }

  private synchronized void sendEmail(String subject, String templateName, Map<String, String> context,
    String recipient) {
    try {
      RestTemplate template = newRestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      StringBuilder form = new StringBuilder(Strings.isNullOrEmpty(recipient) ? "" : recipient + "&");
      form.append("subject=").append(encode(subject, "UTF-8")).append("&template=")
        .append(encode(templateName, "UTF-8"));
      context.forEach((k, v) -> {
        try {
          form.append("&").append(k).append("=").append(encode(v, "UTF-8"));
        } catch(UnsupportedEncodingException ignored) {
        }
      });
      HttpEntity<String> entity = new HttpEntity<>(form.toString(), headers);

      ResponseEntity<String> response = template.exchange(getNotificationsUrl(), HttpMethod.POST, entity, String.class);

      if(response.getStatusCode().is2xxSuccessful()) {
        log.info("Email sent via Agate");
      } else {
        log.error("Sending email via Agate failed with status: {}", response.getStatusCode());
      }
    } catch(Exception e) {
      log.error("Agate connection failure: {}", e.getMessage(), e);
    }
  }

  private synchronized void sendEmail(String subject, String text, String recipient) {
    try {
      RestTemplate template = newRestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      String form = (Strings.isNullOrEmpty(recipient) ? "" : recipient + "&") + "subject=" +
        encode(subject, "UTF-8") + "&body=" + encode(text, "UTF-8");
      HttpEntity<String> entity = new HttpEntity<>(form, headers);

      ResponseEntity<String> response = template.exchange(getNotificationsUrl(), HttpMethod.POST, entity, String.class);

      if(response.getStatusCode().is2xxSuccessful()) {
        log.info("Email sent via Agate");
      } else {
        log.error("Sending email via Agate failed with status: {}", response.getStatusCode());
      }
    } catch(Exception e) {
      log.error("Agate connection failure: {}", e.getMessage(), e);
    }
  }

  private String getApplicationAuth() {
    String token = serviceName + ":" + serviceKey;
    return APPLICATION_AUTH_SCHEMA + " " + Base64.encodeToString(token.getBytes());
  }

  private String getNotificationsUrl() {
    return UriComponentsBuilder.fromHttpUrl(agateUrl).path(DEFAULT_REST_PREFIX).path(DEFAULT_NOTIFICATIONS_PATH).build()
      .toUriString();
  }

  private RestTemplate newRestTemplate() {
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

  private HttpClient createHttpClient() {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    try {
      httpClient.getConnectionManager().getSchemeRegistry()
        .register(new Scheme("https", DEFAULT_HTTPS_PORT, getSocketFactory()));
    } catch(NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }

    return httpClient;
  }

  /**
   * Do not check anything from the remote host (Agate server is trusted).
   *
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   */
  private SchemeSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
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

    return new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
  }
}
