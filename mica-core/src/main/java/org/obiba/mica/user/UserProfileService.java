/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.user;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.service.AgateRestService;
import org.obiba.shiro.realm.ObibaRealm;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
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

  public synchronized Subject getProfile(@NotNull String username) {
    Assert.notNull(username, "Username cannot be null");
    Subject subject = getProfileInternal(getProfileServiceUrl(username));

    if (subject == null) {
      // return dummy Subject in case communication with Agate failed
      subject = new Subject();
      subject.setUsername(username);
    }

    return subject;
  }

  public synchronized Subject getProfileByApplication(@NotNull String username, @NotNull String application,
    @Nullable String group) {
    Assert.notNull(username, "Username cannot be null");
    Assert.notNull(application, "Application name cannot be null");
    return getProfileInternal(getProfileServiceUrlByApp(username, application, group));
  }

  public synchronized List<Subject> getProfilesByApplication(@NotNull String application, @Nullable String group) {

    Assert.notNull(application, "Application name cannot be null");

    try {

      String profileServiceUrl = getProfileServiceUrlByApp(null, application, group);
      return Arrays.asList(executeQuery(profileServiceUrl, Subject[].class));

    } catch(RestClientException e) {
      log.error("Agate connection failure: {}", e.getMessage());
    }

    return Lists.newArrayList();
  }

  public String getUserProfileTranslations(String locale) {

    String serviceUrl = UriComponentsBuilder
      .fromHttpUrl(getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path("/users/i18n/" + locale + ".json")
      .build().toUriString();

    return executeQuery(serviceUrl, String.class);
  }

  public boolean currentUserIs(@NotNull String role) {
    org.apache.shiro.subject.Subject subject = SecurityUtils.getSubject();
    if (subject == null || subject.getPrincipal() == null) {
      return false;
    }

    String username = subject.getPrincipal().toString();

    if (username.equals("administrator")) { return true; }

    ObibaRealm.Subject profile = getProfile(username);
    return profile != null && profile.getGroups() != null && profile.getGroups().stream().filter(g -> g.equals(role)).count() > 0;
  }

  private synchronized Subject getProfileInternal(String serviceUrl) {
    try {
      return executeQuery(serviceUrl, Subject.class);
    } catch(RestClientException e) {
      log.error("Agate connection failure: {}", e.getMessage());
    }

    return null;
  }

  private <T> T executeQuery(String serviceUrl, Class<T> returnType) {

    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
    HttpEntity<String> entity = new HttpEntity<>(null, headers);
    ResponseEntity<T> response = template.exchange(serviceUrl, HttpMethod.GET, entity, returnType);

    return response.getBody();
  }

  private String getProfileServiceUrl(String username) {
    return UriComponentsBuilder.fromHttpUrl(getAgateUrl()).path(DEFAULT_REST_PREFIX).path(
      String.format("/tickets/subject/%s", username)).build().toUriString();
  }

  private String getProfileServiceUrlByApp(String username, String application, String group) {
    UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(getAgateUrl()).path(DEFAULT_REST_PREFIX);

    if (Strings.isNullOrEmpty(username)) {
      urlBuilder.path(String.format("/application/%s/users", application));
    } else {
      urlBuilder.path(String.format("/application/%s/user/%s", application, username));
    }

    if (!Strings.isNullOrEmpty(group)) {
      urlBuilder.queryParam("group", group);
    }

    return urlBuilder.build().toUriString();
  }
}
