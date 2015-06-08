package org.obiba.mica.user;

import org.obiba.mica.core.service.AgateRestService;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UserProfileService extends AgateRestService {
  private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

  @Override
  public void init() {
    initInternal();
  }

  public synchronized Subject getProfile(String username) {
    try {
      RestTemplate template = newRestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
      HttpEntity<String> entity = new HttpEntity<String>(null, headers);
      ResponseEntity<Subject> response = template.exchange(getServiceUrl(username), HttpMethod.GET, entity, Subject.class);
      Subject subject = response.getBody();
      return subject;
    } catch(RestClientException e) {
      log.error("Agate connection failure: {}", e.getMessage(), e);
    }

    return null;
  }

  private String getServiceUrl(String username) {
    return UriComponentsBuilder.fromHttpUrl(getAgateUrl()).path(DEFAULT_REST_PREFIX).path(
      String.format("/tickets/subject/%s", username)).build().toUriString();
  }

}
