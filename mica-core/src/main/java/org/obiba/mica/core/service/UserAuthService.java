package org.obiba.mica.core.service;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.oidc.OIDCAuthProviderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * User authentication, as delegated to Agate.
 */
@Component
public class UserAuthService extends AgateRestService {

  private static final Logger log = LoggerFactory.getLogger(UserAuthService.class);

  private static final String CLIENT_CONFIGURATION_CACHE_KEY = "clientConfiguration";

  private final Cache<String, JSONObject> clientConfigurationCache = CacheBuilder.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

  public synchronized List<OIDCAuthProviderSummary> getOidcProviders(String locale) {
    return getOidcProviders(locale, false);
  }

  public synchronized List<OIDCAuthProviderSummary> getOidcProviders(String locale, boolean signupOnly) {
    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    try {
      ResponseEntity<List<OIDCAuthProviderSummary>> response =
        template.exchange(
          getProvidersUrl(locale, signupOnly),
          HttpMethod.GET,
          entity,
          new ParameterizedTypeReference<List<OIDCAuthProviderSummary>>() {
          }
        );

      return response.getBody();
    } catch (Exception e) {
      log.warn("Cannot get OIDC providers: {}", e.getMessage());
      if (log.isDebugEnabled())
        log.debug("Cannot get OIDC providers", e);
      return Lists.newArrayList();
    }
  }

  public synchronized JSONObject getPublicConfiguration() {
    JSONObject config = getJSONObject(getPublicConfigurationUrl());
    if (!config.has("publicUrl")) { // publicUrl as configured in Agate
      try {
        // publicUrl as configured in Mica
        config.put("publicUrl", agateServerConfigService.getAgateUrl());
      } catch (JSONException e) {
        // ignore
      }
    }
    return config;
  }

  public synchronized JSONObject getClientConfiguration() {
    JSONObject config;
    try {
      config = clientConfigurationCache.get(CLIENT_CONFIGURATION_CACHE_KEY, () -> getJSONObject(getClientConfigurationUrl()));
    } catch (ExecutionException e) {
      log.warn("Cannot get Agate client configuration: {}", e.getMessage());
      if (log.isDebugEnabled())
        log.debug("Cannot get Agate client configuration", e);
      return new JSONObject();
    }
    if (config.isEmpty()) {
      // do not cache a failed/empty fetch, so that the next call retries against Agate instead of
      // waiting out the full cache TTL (which would otherwise wrongly disable reCAPTCHA for 5 minutes)
      clientConfigurationCache.invalidate(CLIENT_CONFIGURATION_CACHE_KEY);
    }
    return config;
  }

  /**
   * Whether a reCAPTCHA site key is configured in Agate. When not configured, forms must not require a captcha input.
   */
  public synchronized boolean isReCaptchaEnabled() {
    try {
      return !Strings.isNullOrEmpty(getClientConfiguration().getString("reCaptchaKey"));
    } catch (JSONException e) {
      return false;
    }
  }

  //
  // Private methods
  //

  private synchronized JSONObject getJSONObject(String url) {
    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
    headers.set("Accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    try {
      ResponseEntity<String> response =
        template.exchange(
          url,
          HttpMethod.GET,
          entity,
          new ParameterizedTypeReference<String>(){}
        );
      return new JSONObject(response.getBody());
    } catch (Exception e) {
      if (log.isDebugEnabled())
        log.debug("Cannot get JSON object from {}", url, e);
      return new JSONObject();
    }
  }

  private String getProvidersUrl(String locale, boolean signupOnly) {
    return UriComponentsBuilder
      .fromUri(agateServerConfigService.getAgateUri())
      .path(DEFAULT_REST_PREFIX)
      .path("/auth/providers")
      .queryParam("locale", locale)
      .queryParam("usage", signupOnly ? "SIGNUP" : "ALL")
      .build().toUriString();
  }

  private String getPublicConfigurationUrl() {
    return UriComponentsBuilder
      .fromUri(agateServerConfigService.getAgateUri())
      .path(DEFAULT_REST_PREFIX)
      .path("/config/_public")
      .build().toUriString();
  }

  private String getClientConfigurationUrl() {
    return UriComponentsBuilder
      .fromUri(agateServerConfigService.getAgateUri())
      .path(DEFAULT_REST_PREFIX)
      .path("/config/client")
      .build().toUriString();
  }

}
