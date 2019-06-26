package org.obiba.mica.core.service;

import org.obiba.oidc.OIDCAuthProviderSummary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class OidcProvidersService extends AgateRestService {

  public synchronized List<OIDCAuthProviderSummary> getProviders(String locale) {
    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
    HttpEntity<String> entity = new HttpEntity<>(null, headers);
    ResponseEntity<List<OIDCAuthProviderSummary>> response =
      template.exchange(
        getProfileServiceUrl(locale),
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<OIDCAuthProviderSummary>>(){}
      );

    return response.getBody();
  }

  private String getProfileServiceUrl(String locale) {
    return UriComponentsBuilder
      .fromHttpUrl(agateServerConfigService.getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path("/auth/providers")
      .queryParam("locale", locale)
      .build().toUriString();
  }

}
